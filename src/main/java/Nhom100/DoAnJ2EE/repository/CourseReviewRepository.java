package Nhom100.DoAnJ2EE.repository;

import Nhom100.DoAnJ2EE.entity.CourseReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;

public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {
    List<CourseReview> findByCourseIdOrderByCreatedAtDesc(Long courseId);
    Optional<CourseReview> findByCourseIdAndUserId(Long courseId, Long userId);

    interface CourseRatingSummary {
        Long getCourseId();
        Double getAvgRating();
        Long getTotalReviews();
    }

    @Query("select r.course.id as courseId, avg(r.rating) as avgRating, count(r.id) as totalReviews " +
           "from CourseReview r group by r.course.id")
    List<CourseRatingSummary> findCourseRatings();

    @Query("select r.course.id as courseId, avg(r.rating) as avgRating, count(r.id) as totalReviews " +
           "from CourseReview r group by r.course.id " +
           "order by avg(r.rating) desc, count(r.id) desc")
    List<CourseRatingSummary> findTopRatedCourses();
}
