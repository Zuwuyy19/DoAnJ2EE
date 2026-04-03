package Nhom100.DoAnJ2EE.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import Nhom100.DoAnJ2EE.entity.Chapter;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    List<Chapter> findByCourseIdOrderById(Long courseId);
    boolean existsByCourseId(Long courseId);
}
