package Nhom100.DoAnJ2EE.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Entity Order - đại diện cho một đơn hàng (mua khóa học)
 * Mỗi order sẽ ghi nhận user nào mua khóa học nào, vào lúc nào, giá bao nhiêu
 */
@Data   // Lombok: tự động sinh getter/setter/toString/equals/hashCode
@Entity   // Đánh dấu đây là một entity JPA, map với bảng trong DB
@Table(name = "orders")   // Tên bảng trong database là "orders"
public class Order {

    @Id   // Khóa chính
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // ID tự động tăng (auto-increment)
    private Long id;

    // Người dùng đặt mua khóa học này
    // Quan hệ N-1: nhiều order có thể thuộc về 1 user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")   // Cột khóa ngoại trong bảng orders trỏ đến bảng users
    private User user;

    // Khóa học được đặt mua trong đơn hàng này
    // Quan hệ N-1: nhiều order có thể đặt mua cùng 1 khóa học
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id")   // Cột khóa ngoại trong bảng orders trỏ đến bảng courses
    private Course course;

    // Giá tại thời điểm mua (có thể khác giá hiện tại của khóa học)
    private Double price;

    // Thời điểm đặt mua, mặc định là thời gian hiện tại khi tạo đơn
    @Column(name = "order_date")   // Tên cột trong DB là "order_date"
    private LocalDateTime orderDate = LocalDateTime.now();

    // Trạng thái đơn hàng: PENDING (chờ xử lý), COMPLETED (hoàn thành), CANCELLED (hủy)
    // Mặc định khi tạo đơn là PENDING
    @Column(name = "status")
    private String status = "PENDING";

    // Constructor mặc định (bắt buộc cho JPA)
    public Order() {}

    // Constructor có tham số để khởi tạo nhanh đơn hàng
    public Order(User user, Course course, Double price) {
        this.user = user;               // Gán người dùng cho đơn hàng
        this.course = course;           // Gán khóa học cho đơn hàng
        this.price = price;             // Gán giá tại thời điểm mua
        this.orderDate = LocalDateTime.now();   // Gán thời gian hiện tại
        this.status = "PENDING";        // Mặc định trạng thái là chờ xử lý
    }
}
