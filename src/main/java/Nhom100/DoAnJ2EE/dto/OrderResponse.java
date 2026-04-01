package Nhom100.DoAnJ2EE.dto;

import Nhom100.DoAnJ2EE.entity.Course;
import java.time.LocalDateTime;

/**
 * DTO (Data Transfer Object) cho response trả về khi lấy danh sách khóa học đã mua
 * Không trả về toàn bộ entity Order để tránh lộ thông tin nhạy cảm (ví dụ: userId)
 */
public class OrderResponse {

    // ID của đơn hàng
    private Long orderId;

    // Thông tin khóa học đã mua (entity Course)
    // Trả về course để frontend hiển thị thông tin khóa học cho user
    private Course course;

    // Giá tại thời điểm mua
    private Double price;

    // Ngày giờ đặt mua
    private LocalDateTime orderDate;

    // Trạng thái đơn hàng (PENDING, COMPLETED, CANCELLED)
    private String status;

    // Constructor mặc định
    public OrderResponse() {}

    // Getter cho orderId - lấy ID đơn hàng
    public Long getOrderId() {
        return orderId;
    }

    // Setter cho orderId - gán ID đơn hàng
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    // Getter cho course - lấy thông tin khóa học
    public Course getCourse() {
        return course;
    }

    // Setter cho course - gán thông tin khóa học
    public void setCourse(Course course) {
        this.course = course;
    }

    // Getter cho price - lấy giá tại thời điểm mua
    public Double getPrice() {
        return price;
    }

    // Setter cho price - gán giá
    public void setPrice(Double price) {
        this.price = price;
    }

    // Getter cho orderDate - lấy ngày giờ đặt mua
    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    // Setter cho orderDate - gán ngày giờ đặt mua
    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    // Getter cho status - lấy trạng thái đơn hàng
    public String getStatus() {
        return status;
    }

    // Setter cho status - gán trạng thái đơn hàng
    public void setStatus(String status) {
        this.status = status;
    }
}
