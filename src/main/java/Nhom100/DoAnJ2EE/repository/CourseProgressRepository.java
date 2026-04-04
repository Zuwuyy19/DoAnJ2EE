package Nhom100.DoAnJ2EE.repository;

import Nhom100.DoAnJ2EE.entity.CourseProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseProgressRepository extends JpaRepository<CourseProgress, Long> {

    List<CourseProgress> findByUserIdAndCourseId(@Param("userId") Long userId, @Param("courseId") Long courseId);

    Optional<CourseProgress> findByUserIdAndLessonId(@Param("userId") Long userId, @Param("lessonId") Long lessonId);

    boolean existsByUserIdAndLessonId(@Param("userId") Long userId, @Param("lessonId") Long lessonId);

    @Query("SELECT COUNT(cp) FROM CourseProgress cp " +
           "WHERE cp.user.id = :userId AND cp.course.id = :courseId AND cp.completed = true")
    long countCompletedLessons(@Param("userId") Long userId, @Param("courseId") Long courseId);

    void deleteByUserIdAndCourseId(@Param("userId") Long userId, @Param("courseId") Long courseId);
}
