package Nhom100.DoAnJ2EE.service;

import Nhom100.DoAnJ2EE.dto.CreateOrderRequest;
import Nhom100.DoAnJ2EE.dto.OrderResponse;
import java.util.List;

/**
 * Interface Service cho chức năng Order (quản lý đơn hàng mua khóa học)
 * Định nghĩa các method nghiệp vụ cần thiết:
 * - Tạo đơn hàng mới (mua khóa học)
 * - Lấy danh sách khóa học đã mua của một user
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
}
