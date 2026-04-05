package Nhom100.DoAnJ2EE.service;

import Nhom100.DoAnJ2EE.dto.CreateOrderRequest;
import Nhom100.DoAnJ2EE.dto.OrderResponse;
import Nhom100.DoAnJ2EE.entity.Order;
import java.util.List;
import java.util.Map;

/**
 * Interface Service cho chức năng Order (quản lý đơn hàng mua khóa học)
 * Định nghĩa các method nghiệp vụ cần thiết:
 * - Tạo đơn hàng mới (mua khóa học)
 * - Lấy danh sách khóa học đã mua của một user
 * - Tạo đơn hàng VNPay (chờ thanh toán)
 * - Xử lý phản hồi VNPay (cập nhật trạng thái đơn)
 */
public interface OrderService {

    /**
     * Tạo đơn hàng mới (mua khóa học)
     * Kiểm tra khóa học tồn tại, kiểm tra chưa mua rồi, rồi tạo đơn hàng
     * @param userId ID của người dùng đặt mua (lấy từ JWT token)
     * @param request DTO chứa courseId cần mua
     * @return OrderResponse chứa thông tin đơn hàng vừa tạo, hoặc null nếu thất bại
     */
    OrderResponse createOrder(Long userId, CreateOrderRequest request);

    /**
     * Lấy danh sách khóa học đã mua của một người dùng
     * Trả về danh sách đơn hàng kèm thông tin khóa học (đã hoàn thành thanh toán)
     * @param userId ID của người dùng cần tra cứu
     * @return Danh sách OrderResponse chứa thông tin các khóa học đã mua
     */
    List<OrderResponse> getMyCourses(Long userId);

    /**
     * Tạo đơn hàng chờ thanh toán VNPay
     * Sinh mã thanh toán, lưu order PENDING, trả về URL thanh toán VNPay
     * @param userId ID người dùng
     * @param courseId ID khóa học
     * @param ipAddress IP người dùng
     * @return Map chứa orderId, paymentCode, paymentUrl
     */
    Map<String, Object> createVNPayOrder(Long userId, Long courseId, String ipAddress);

    /**
     * Xử lý phản hồi thành công từ VNPay (return URL)
     * Xác minh hash, cập nhật order → PAID nếu hợp lệ
     * @param params toàn bộ params từ VNPay gửi về
     * @return Order đã cập nhật, hoặc null nếu thất bại
     */
    Order handleVNPayReturn(Map<String, String> params);

    /**
     * Tìm đơn hàng theo ID
     */
    Order getOrderById(Long orderId);

    /**
     * Tìm đơn hàng theo mã thanh toán (VNPay)
     */
    Order getOrderByPaymentCode(String paymentCode);
}
