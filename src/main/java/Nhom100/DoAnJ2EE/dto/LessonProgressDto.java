package Nhom100.DoAnJ2EE.dto;

import java.time.LocalDateTime;

/**
 * DTO cho trạng thái hoàn thành của một bài giảng.
 */
public class LessonProgressDto {

    private Long lessonId;
    private boolean completed;
    private LocalDateTime completedAt;

    public LessonProgressDto() {}

    public LessonProgressDto(Long lessonId, boolean completed, LocalDateTime completedAt) {
        this.lessonId = lessonId;
        this.completed = completed;
        this.completedAt = completedAt;
    }

    public Long getLessonId() {
        return lessonId;
    }

    public void setLessonId(Long lessonId) {
        this.lessonId = lessonId;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
