package Nhom100.DoAnJ2EE.repository;

import Nhom100.DoAnJ2EE.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
    List<OrderDetail> findByOrderUserId(Long userId);

    @Query("SELECT COUNT(od) > 0 FROM OrderDetail od " +
           "WHERE od.order.user.id = :userId " +
           "AND od.course.id = :courseId " +
           "AND od.order.status IN ('PAID', 'COMPLETED')")
    boolean existsByOrderUserIdAndCourseId(Long userId, @Param("courseId") Long courseId);

    @Query("SELECT COUNT(DISTINCT od.order.user.id) FROM OrderDetail od WHERE od.course.id = :courseId")
    long countDistinctStudentsByCourseId(@Param("courseId") Long courseId);

    void deleteByOrderId(Long orderId);
}
