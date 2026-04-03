package Nhom100.DoAnJ2EE.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import Nhom100.DoAnJ2EE.entity.Lesson;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    List<Lesson> findByChapterIdOrderById(Long chapterId);
}
