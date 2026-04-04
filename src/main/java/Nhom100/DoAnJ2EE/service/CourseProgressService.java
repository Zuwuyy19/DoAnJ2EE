package Nhom100.DoAnJ2EE.service;

import Nhom100.DoAnJ2EE.dto.CourseProgressDto;
import Nhom100.DoAnJ2EE.dto.LessonProgressDto;
import java.util.List;
import java.util.Map;

/**
 * Service interface cho chức năng theo dõi tiến độ học tập.
 */
public interface CourseProgressService {

    /**
     * Lấy tổng tiến độ của user trên một khóa học.
     * @param userId ID người dùng
     * @param courseId ID khóa học
     * @return CourseProgressDto chứa % hoàn thành và danh sách bài đã học
     */
    CourseProgressDto getCourseProgress(Long userId, Long courseId);

    /**
     * Đánh dấu một bài giảng là đã hoàn thành.
     * Nếu record đã tồn tại → cập nhật completed=true và completedAt.
     * @param userId ID người dùng
     * @param lessonId ID bài giảng
     * @param courseId ID khóa học
     */
    void markLessonCompleted(Long userId, Long lessonId, Long courseId);

    /**
     * Bỏ đánh dấu hoàn thành một bài giảng.
     * @param userId ID người dùng
     * @param lessonId ID bài giảng
     */
    void markLessonIncomplete(Long userId, Long lessonId);

    /**
     * Đếm số bài đã hoàn thành của user trong một khóa học.
     * @param userId ID người dùng
     * @param courseId ID khóa học
     * @return số bài đã hoàn thành
     */
    long countCompletedLessons(Long userId, Long courseId);

    /**
     * Lấy map lessonId → completed cho một khóa học.
     * Dùng để render sidebar với ✓/○ từng bài.
     * @param userId ID người dùng
     * @param courseId ID khóa học
     * @return Map<Long lessonId, Boolean completed>
     */
    Map<Long, Boolean> getLessonCompletedMap(Long userId, Long courseId);

    /**
     * Lấy danh sách LessonProgressDto cho một khóa học.
     * @param userId ID người dùng
     * @param courseId ID khóa học
     * @return danh sách tiến độ từng bài
     */
    List<LessonProgressDto> getLessonProgressList(Long userId, Long courseId);
}
