package Nhom100.DoAnJ2EE.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import Nhom100.DoAnJ2EE.entity.Course;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByCategoryId(Long categoryId);
}
