package Nhom100.DoAnJ2EE.controller;

import Nhom100.DoAnJ2EE.config.JwtUtil;
import Nhom100.DoAnJ2EE.dto.CreateOrderRequest;
import Nhom100.DoAnJ2EE.dto.OrderResponse;
import Nhom100.DoAnJ2EE.entity.User;
import Nhom100.DoAnJ2EE.repository.UserRepository;
import Nhom100.DoAnJ2EE.service.OrderService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * OrderController - REST Controller xử lý các API liên quan đến đơn hàng (mua khóa học)
 * @RestController = @Controller + @ResponseBody
 *   - @Controller: đánh dấu đây là một Spring MVC Controller xử lý HTTP request
 *   - @ResponseBody: tự động serialize return value thành JSON/XML và gửi về client
 * @RequestMapping("/api/orders"): tiền tố URL cho tất cả endpoints trong controller này
 * Ví dụ: http://localhost:8080/api/orders, http://localhost:8080/api/orders/my-courses
 */
@RestController   // Đánh dấu đây là REST Controller - trả về dữ liệu (JSON) thay vì view (HTML)
@RequestMapping("/api/orders")   // Tiền tố URL cho tất cả endpoint: /api/orders
public class OrderController {

    // Inject OrderService - chứa logic nghiệp vụ xử lý đơn hàng
    // Service được Spring quản lý thông qua @Autowired injection
    @Autowired
    private OrderService orderService;

    // Inject UserRepository - dùng để tra cứu thông tin user từ email (lấy từ JWT)
    @Autowired
    private UserRepository userRepository;

    /**
     * API tạo đơn hàng mới (mua khóa học)
     * Method: POST
     * URL: /api/orders
     * Headers: Authorization: Bearer <jwt_token>
     * Body: { "courseId": <id_khóa_học> }
     *
     * Quy trình xử lý:
     *  1. Xác thực user từ JWT token trong header
     *  2. Parse request body để lấy courseId
     *  3. Gọi OrderService để tạo đơn hàng
     *  4. Trả về thông tin đơn hàng vừa tạo (201 Created) hoặc lỗi (400/404/409)
     */
    @PostMapping   // Ánh xạ method này với HTTP POST request đến /api/orders
    public ResponseEntity<?> createOrder(
            // @RequestHeader("Authorization"): lấy giá trị header "Authorization"
            // Ví dụ: "Bearer eyJhbGciOiJIUzI1NiJ9..."
            @RequestHeader("Authorization") String authHeader,

            // @RequestBody: tự động deserialize JSON body thành object CreateOrderRequest
            // Jackson sẽ parse request body JSON → đối tượng Java CreateOrderRequest
            @RequestBody CreateOrderRequest request
    ) {
        try {
            // Bước 1: Xác thực JWT token và lấy email của user đang đăng nhập
            // authHeader chứa "Bearer <token>", ta cắt bỏ prefix "Bearer " để lấy token
            String token = authHeader.substring(7);   // Bỏ 7 ký tự "Bearer "

            // JwtUtil.getEmail(token): parse token, verify signature và expiration,
            // nếu hợp lệ → trả về email (subject) được lưu trong token
            // Nếu token không hợp lệ hoặc hết hạn → ném exception
            String email = JwtUtil.getEmail(token);

            // Bước 2: Tra cứu user trong DB theo email lấy được từ token
            // Lấy userId thực tế từ database để đảm bảo user tồn tại và lấy đúng ID
            User user = userRepository.findByEmail(email).orElse(null);

            // Bước 3: Kiểm tra user có tồn tại trong DB không
            // Nếu không tìm thấy → trả về lỗi 401 Unauthorized
            if (user == null) {
                // ResponseEntity: cho phép tùy chỉnh HTTP status code và body
                // HttpStatus.UNAUTHORIZED (401): client chưa xác thực hoặc xác thực thất bại
                // Map<String, String>: tạo JSON body { "error": "..." }
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Người dùng không tồn tại trong hệ thống"));
            }

            // Bước 4: Gọi OrderService để tạo đơn hàng
            // Service sẽ kiểm tra khóa học tồn tại, chưa mua rồi, rồi tạo đơn
            // Trả về OrderResponse chứa thông tin đơn hàng vừa tạo
            OrderResponse order = orderService.createOrder(user.getId(), request);

            // Bước 5: Trả về phản hồi thành công
            // HttpStatus.CREATED (201): tài nguyên mới đã được tạo thành công
            // Body chứa OrderResponse (Spring sẽ tự động serialize thành JSON)
            return ResponseEntity.status(HttpStatus.CREATED).body(order);

        } catch (IllegalArgumentException e) {
            // Xử lý lỗi: không tìm thấy user hoặc khóa học
            // IllegalArgumentException được ném từ OrderService khi không tìm thấy entity
            // Trả về 404 Not Found - tài nguyên không tồn tại
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));   // Trả về message lỗi từ service

        } catch (IllegalStateException e) {
            // Xử lý lỗi: vi phạm nghiệp vụ (ví dụ: đã mua khóa học rồi)
            // IllegalStateException được ném từ OrderService khi vi phạm quy tắc nghiệp vụ
            // Trả về 409 Conflict - request xung đột với trạng thái hiện tại của server
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));   // Trả về message lỗi từ service

        } catch (Exception e) {
            // Xử lý lỗi không xác định (Unexpected error)
            // Ví dụ: lỗi kết nối database, lỗi JWT parsing...
            // Trả về 500 Internal Server Error - lỗi phía server
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Đã xảy ra lỗi khi xử lý yêu cầu: " + e.getMessage()));
        }
    }

    /**
     * API lấy danh sách khóa học đã mua của user hiện tại
     * Method: GET
     * URL: /api/orders/my-courses
     * Headers: Authorization: Bearer <jwt_token>
     *
     * Quy trình xử lý:
     *  1. Xác thực user từ JWT token trong header
     *  2. Gọi OrderService để lấy danh sách đơn hàng đã hoàn thành của user
     *  3. Trả về danh sách OrderResponse (200 OK) hoặc lỗi (401)
     */
    @GetMapping("/my-courses")   // Ánh xạ HTTP GET request đến /api/orders/my-courses
    public ResponseEntity<?> getMyCourses(
            // @RequestHeader("Authorization"): lấy JWT token từ header Authorization
            // Nếu header không tồn tại → Spring sẽ ném MissingRequestHeaderException
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            // Bước 1: Xác thực JWT token và lấy email của user đang đăng nhập
            // Tương tự như trong createOrder - trích xuất email từ token
            String token = authHeader.substring(7);   // Bỏ prefix "Bearer "

            // JwtUtil.getEmail(token): parse và verify token
            // Nếu token hết hạn hoặc chữ ký sai → ném exception
            String email = JwtUtil.getEmail(token);

            // Bước 2: Tra cứu user trong DB theo email
            // Lấy user entity để có userId thực tế
            User user = userRepository.findByEmail(email).orElse(null);

            // Bước 3: Kiểm tra user tồn tại trong DB
            // Nếu không tìm thấy → trả về lỗi 401 Unauthorized
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Người dùng không tồn tại trong hệ thống"));
            }

            // Bước 4: Gọi OrderService để lấy danh sách khóa học đã mua
            // getMyCourses(userId): trả về danh sách OrderResponse
            // Mỗi OrderResponse chứa thông tin đơn hàng + thông tin khóa học
            List<OrderResponse> myCourses = orderService.getMyCourses(user.getId());

            // Bước 5: Trả về phản hồi thành công
            // HttpStatus.OK (200): request thành công, trả về dữ liệu
            // Body: danh sách OrderResponse (Spring tự động serialize thành JSON array)
            return ResponseEntity.ok(myCourses);

        } catch (Exception e) {
            // Xử lý lỗi không xác định
            // Ví dụ: token hết hạn, lỗi database, lỗi JWT parsing...
            // Trả về 500 Internal Server Error
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Đã xảy ra lỗi khi xử lý yêu cầu: " + e.getMessage()));
        }
    }
}
