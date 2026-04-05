package Nhom100.DoAnJ2EE.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import Nhom100.DoAnJ2EE.service.CustomUserDetailsService;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Cấu hình Spring Security cho toàn bộ ứng dụng
 * @Configuration: Đánh dấu đây là class cấu hình Spring, được load khi ứng dụng khởi động
 */
@Configuration   // Đánh dấu đây là class cấu hình Spring
@EnableMethodSecurity // Kích hoạt Method Security (@PreAuthorize, v.v...)
public class SecurityConfig {

    // Inject JwtAuthenticationFilter - filter xử lý JWT authentication cho REST API
    // Filter này sẽ kiểm tra JWT token trong header Authorization
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // Inject CustomUserDetailsService - dùng cho form login (web)
    @Autowired
    private CustomUserDetailsService userDetailsService;

    /**
     * Bean PasswordEncoder - dùng để mã hóa và kiểm tra password
     * @Bean: Đăng ký method này như một Spring Bean trong IoC Container
     * BCryptPasswordEncoder: Thuật toán mã hóa password mạnh, tự động thêm salt
     * Sử dụng khi: đăng ký user mới (mã hóa password trước khi lưu) và đăng nhập (so sánh password)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();   // Trả về BCrypt encoder để mã hóa password
    }

    /**
     * Bean SecurityFilterChain - cấu hình chuỗi filter bảo mật cho mỗi request
     * Đây là cấu hình chính của Spring Security
     * @Bean: Đăng ký filter chain vào Spring Container
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            // ===== CẤU HÌNH CSRF =====
            // CSRF (Cross-Site Request Forgery): tấn công giả mạo request từ trang khác
            // .csrf(csrf -> csrf.disable()): TẮT CSRF protection
            // Vì đây là REST API dùng JWT (stateless), CSRF token không cần thiết
            // CSRF chỉ cần thiết khi dùng session-based authentication (cookie)
            .csrf(csrf -> csrf.disable())

            // ===== CẤU HÌNH PHÂN QUYỀN REQUEST =====
            .authorizeHttpRequests(auth -> auth

                // 🌐 Public endpoints - KHÔNG cần đăng nhập:
                .requestMatchers(
                        "/",                          // Trang chủ
                        "/login",                     // Trang đăng nhập web
                        "/register",                  // Trang đăng ký web
                        "/auth/**",                   // Tất cả endpoint auth (login, register)
                        "/css/**",                    // File CSS tĩnh
                        "/js/**",                     // File JavaScript tĩnh
                        "/images/**",                 // File ảnh tĩnh
                        "/swagger-ui/**",              // Swagger UI
                        "/v3/api-docs/**",             // OpenAPI JSON
                        "/vnpay/**"                    // VNPay payment (return & IPN)
                ).permitAll()   // Cho phép tất cả mọi người truy cập

                // 🚀 Endpoints dành riêng cho Admin (Trang web):
                // Chỉ user có "ROLE_ADMIN" mới được vào các URL bắt đầu bằng /admin
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // 🔐 REST API endpoints - CẦN xác thực bằng JWT:
                // Tất cả request bắt đầu bằng "/api/" đều yêu cầu authentication
                // JWT Filter sẽ kiểm tra token và xác thực user trước khi vào Controller
                .requestMatchers("/api/**").authenticated()

                // 🔒 Các request khác (không phải public hoặc /api/**) - CẦN đăng nhập:
                // Bao gồm các trang web sử dụng form login thông thường
                .anyRequest().authenticated()   // Tất cả request còn lại cần xác thực
            )

            // ===== CẤU HÌNH FORM LOGIN (CHO WEB) =====
            // Cấu hình đăng nhập bằng form (dùng session-based authentication)
            // Phục vụ cho web UI sử dụng HTML form thay vì REST API
            .userDetailsService(userDetailsService)   // Load user details để xác thực
            .formLogin(form -> form
                    .loginPage("/login")              // Trang login tùy chỉnh
                    .loginProcessingUrl("/login")   // URL xử lý submit form login
                    .defaultSuccessUrl("/", true)     // Sau khi login thành công → chuyển đến trang chủ
                    .failureUrl("/login?error=true") // Login thất bại → quay lại trang login với tham số error
                    .permitAll()                      // Cho phép tất cả người dùng truy cập trang login
            )

            // ===== CẤU HÌNH LOGOUT =====
            // Xử lý đăng xuất: xóa session, xóa context, chuyển hướng về trang login
            .logout(logout -> logout
                    .logoutUrl("/logout")           // URL触发 logout (POST request)
                    .logoutSuccessUrl("/login?logout=true")  // Sau khi logout → trang login với tham số logout
                    .permitAll()                     // Cho phép tất cả người dùng logout
            )

            // ===== THÊM JWT FILTER VÀO CHUỖI FILTER =====
            // jwtAuthenticationFilter sẽ chạy TRƯỚC UsernamePasswordAuthenticationFilter
            // Nó kiểm tra JWT token trong header Authorization của request
            // Nếu token hợp lệ → đặt Authentication vào SecurityContext
            // Nếu không có token hoặc token không hợp lệ → cho request đi tiếp (Spring Security sẽ xử lý)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Build và trả về SecurityFilterChain đã được cấu hình
        return http.build();
    }
}
