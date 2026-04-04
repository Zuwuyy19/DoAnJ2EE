package Nhom100.DoAnJ2EE.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.time.LocalDateTime;

/**
 * Entity theo dõi tiến độ học tập của user trên từng bài giảng.
 * Unique constraint: mỗi user chỉ có 1 record cho mỗi lesson.
 */
@Data
@Entity
@Table(name = "course_progress",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "lesson_id"}))
public class CourseProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Lesson lesson;

    @Column(nullable = false)
    private boolean completed = false;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public CourseProgress() {}

    public CourseProgress(User user, Course course, Lesson lesson) {
        this.user = user;
        this.course = course;
        this.lesson = lesson;
        this.completed = false;
    }

    public CourseProgress(User user, Course course, Lesson lesson, boolean completed) {
        this.user = user;
        this.course = course;
        this.lesson = lesson;
        this.completed = completed;
    }
}
