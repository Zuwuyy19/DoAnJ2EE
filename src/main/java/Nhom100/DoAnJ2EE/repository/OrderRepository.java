package Nhom100.DoAnJ2EE.repository;

import Nhom100.DoAnJ2EE.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository cho entity Order - cung cấp các thao tác CRUD với bảng orders trong DB
 * JpaRepository đã cung cấp sẵn: save, findById, findAll, deleteById, existsById...
 */
@Repository   // Đánh dấu đây là một Spring Repository, tự động được quản lý bởi Spring Container
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Tìm tất cả đơn hàng của một người dùng, kèm thông tin khóa học (sử dụng JOIN FETCH)
     * JOIN FETCH giúp tránh N+1 query: lấy order + course trong 1 câu SQL duy nhất
     * @param userId ID của người dùng
     * @return Danh sách đơn hàng đã được eager load thông tin khóa học
     */
    @Query("SELECT o FROM Order o JOIN FETCH o.course WHERE o.user.id = :userId")
    List<Order> findByUserIdWithCourse(@Param("userId") Long userId);

    /**
     * Kiểm tra xem một người dùng đã mua một khóa học cụ thể chưa
     * @param userId ID của người dùng
     * @param courseId ID của khóa học cần kiểm tra
     * @return true nếu đã mua rồi, false nếu chưa
     */
    boolean existsByUserIdAndCourseId(Long userId, Long courseId);
}
