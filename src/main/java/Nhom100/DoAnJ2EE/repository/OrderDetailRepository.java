package Nhom100.DoAnJ2EE.repository;

import Nhom100.DoAnJ2EE.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
    List<OrderDetail> findByOrderUserId(Long userId);
    boolean existsByOrderUserIdAndCourseId(Long userId, Long courseId);

    @Query("SELECT CASE WHEN COUNT(od) > 0 THEN true ELSE false END FROM OrderDetail od " +
           "WHERE od.order.user.id = :userId AND od.course.id = :courseId " +
           "AND (od.order.status = 'COMPLETED' OR od.order.status = 'PAID')")
    boolean existsCompletedOrderByUserAndCourse(@Param("userId") Long userId, @Param("courseId") Long courseId);

    @Modifying
    @Query("DELETE FROM OrderDetail od WHERE od.order.id = :orderId")
    void deleteByOrderId(@Param("orderId") Long orderId);

    @Query("SELECT COUNT(DISTINCT od.order.user.id) FROM OrderDetail od WHERE od.course.id = :courseId")
    long countDistinctStudentsByCourseId(@Param("courseId") Long courseId);

    @Query("SELECT DISTINCT od.course.category.id FROM OrderDetail od " +
           "WHERE od.order.user.id = :userId AND od.course.category IS NOT NULL")
    List<Long> findPurchasedCategoryIds(@Param("userId") Long userId);

    interface CourseTrendingSummary {
        Long getCourseId();
        Long getTotalOrders();
    }

    interface TopCourseSummary {
        Long getCourseId();
        String getTitle();
        Long getTotalOrders();
        Double getRevenue();
    }

    @Query("SELECT od.course.id as courseId, COUNT(od.id) as totalOrders " +
           "FROM OrderDetail od GROUP BY od.course.id ORDER BY COUNT(od.id) DESC")
    List<CourseTrendingSummary> findTrendingCourses();

    @Query("SELECT od.course.id as courseId, od.course.title as title, COUNT(od.id) as totalOrders, SUM(od.price) as revenue " +
           "FROM OrderDetail od GROUP BY od.course.id, od.course.title ORDER BY SUM(od.price) DESC")
    List<TopCourseSummary> findTopCourses();
}
