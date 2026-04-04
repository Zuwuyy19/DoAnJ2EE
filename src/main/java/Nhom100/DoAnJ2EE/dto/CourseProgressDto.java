package Nhom100.DoAnJ2EE.dto;

import java.util.List;

/**
 * DTO tổng hợp tiến độ học tập của user trên một khóa học.
 */
public class CourseProgressDto {

    private Long courseId;
    private long totalLessons;
    private long completedLessons;
    private double percentComplete;
    private List<LessonProgressDto> lessons;

    public CourseProgressDto() {}

    public CourseProgressDto(Long courseId, long totalLessons, long completedLessons,
                             double percentComplete, List<LessonProgressDto> lessons) {
        this.courseId = courseId;
        this.totalLessons = totalLessons;
        this.completedLessons = completedLessons;
        this.percentComplete = percentComplete;
        this.lessons = lessons;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public long getTotalLessons() {
        return totalLessons;
    }

    public void setTotalLessons(long totalLessons) {
        this.totalLessons = totalLessons;
    }

    public long getCompletedLessons() {
        return completedLessons;
    }

    public void setCompletedLessons(long completedLessons) {
        this.completedLessons = completedLessons;
    }

    public double getPercentComplete() {
        return percentComplete;
    }

    public void setPercentComplete(double percentComplete) {
        this.percentComplete = percentComplete;
    }

    public List<LessonProgressDto> getLessons() {
        return lessons;
    }

    public void setLessons(List<LessonProgressDto> lessons) {
        this.lessons = lessons;
    }
}
