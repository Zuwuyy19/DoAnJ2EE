package Nhom100.DoAnJ2EE.dto;

/**
 * DTO (Data Transfer Object) cho request tạo đơn hàng mới
 * Client gửi lên chỉ cần cung cấp courseId, không cần gửi các trường khác
 * vì thông tin user sẽ được lấy từ JWT token đã xác thực
 */
public class CreateOrderRequest {

    // ID của khóa học mà người dùng muốn mua
    // Giá trị này bắt buộc phải có trong request
    private Long courseId;

    // Constructor mặc định (bắt buộc để Jackson deserialize JSON thành object)
    public CreateOrderRequest() {}

    // Getter cho courseId - lấy ID khóa học
    public Long getCourseId() {
        return courseId;
    }

    // Setter cho courseId - gán ID khóa học
    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }
}
