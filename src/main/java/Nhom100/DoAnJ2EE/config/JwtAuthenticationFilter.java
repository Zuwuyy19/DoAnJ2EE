package Nhom100.DoAnJ2EE.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import Nhom100.DoAnJ2EE.entity.User;
import Nhom100.DoAnJ2EE.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.stream.Collectors;

/**
 * JwtAuthenticationFilter - Filter xử lý xác thực JWT token cho REST API
 * Kế thừa OncePerRequestFilter: đảm bảo filter chỉ chạy 1 lần cho mỗi request
 * @Component: Đánh dấu là một Spring Component, tự động được đăng ký vào filter chain
 * Filter này chạy TRƯỚC khi request đến Controller, kiểm tra và xác thực JWT token
 */
@Component   // Spring Component - tự động được quản lý bởi Spring Container
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // Inject UserRepository để tra cứu thông tin user từ DB khi có token hợp lệ
    @Autowired
    private UserRepository userRepository;

    /**
     * Phương thức chính của filter - xử lý mỗi request đến
     * @param request       HttpServletRequest - chứa thông tin request (headers, params, body...)
     * @param response      HttpServletResponse - dùng để gửi phản hồi về client
     * @param filterChain   FilterChain - chứa các filter tiếp theo trong chuỗi
     * @throws ServletException nếu có lỗi liên quan đến servlet
     * @throws IOException nếu có lỗi I/O khi đọc/ghi dữ liệu
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Bước 1: Lấy header "Authorization" từ request
        // Header này chứa JWT token với format: "Bearer <token>"
        String authHeader = request.getHeader("Authorization");

        // Bước 2: Kiểm tra header có tồn tại và bắt đầu bằng "Bearer " không
        // Nếu không có hoặc sai format → bỏ qua xác thực, cho request đi tiếp
        // (Spring Security sẽ từ chối nếu endpoint cần authentication)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);   // Cho request đi tiếp mà không xác thực
            return;   // Kết thúc xử lý filter cho request này
        }

        // Bước 3: Trích xuất token từ header (bỏ prefix "Bearer ")
        // Ví dụ: "Bearer eyJhbGciOiJIUzI1NiJ9..." → "eyJhbGciOiJIUzI1NiJ9..."
        String token = authHeader.substring(7);   // substring(7) bỏ 7 ký tự "Bearer "

        try {
            // Bước 4: Parse và xác thực token bằng JwtUtil
            // JwtUtil.getEmail(token) sẽ decode token và lấy email (subject) của user
            // Nếu token hết hạn hoặc không hợp lệ → ném exception
            String email = JwtUtil.getEmail(token);

            // Bước 5: Tra cứu user trong DB theo email lấy được từ token
            // Dùng userRepository để đảm bảo user tồn tại trong hệ thống
            User user = userRepository.findByEmail(email).orElse(null);

            // Bước 6: Nếu user tồn tại → tạo đối tượng Authentication và đăng ký vào SecurityContext
            // SecurityContext là nơi lưu trữ thông tin user đã xác thực cho request hiện tại
            if (user != null) {
                // Tạo danh sách quyền (authorities) cho user
                // Chuyển đổi Set<Role> từ entity User → danh sách SimpleGrantedAuthority
                var authorities = user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.getName()))   // VD: "ROLE_USER", "ROLE_ADMIN"
                        .collect(Collectors.toList());   // Gom thành danh sách

                // Tạo UsernamePasswordAuthenticationToken - đại diện cho user đã đăng nhập
                // Tham số: (principal, credentials, authorities)
                // principal = email (username), credentials = null (đã xác thực qua token), authorities = quyền
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(email, null, authorities);

                // Đăng ký authentication vào SecurityContextHolder
                // Spring Security sẽ dùng SecurityContext này để kiểm tra quyền truy cập
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

        } catch (Exception e) {
            // Bước 7: Nếu có lỗi (token hết hạn, sai chữ ký, user không tồn tại...)
            // Không làm gì cả, cho request đi tiếp mà không đặt Authentication
            // Spring Security sẽ trả về 401 Unauthorized khi endpoint yêu cầu authentication
            // Log lỗi ra console để debug (có thể thay bằng Logger trong production)
            System.out.println("JWT Authentication failed: " + e.getMessage());
        }

        // Bước 8: Cho request đi tiếp trong filter chain
        // Filter tiếp theo sẽ xử lý hoặc chuyển request đến Controller
        filterChain.doFilter(request, response);
    }
}
