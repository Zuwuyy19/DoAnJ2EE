package Nhom100.DoAnJ2EE.service.impl;

import Nhom100.DoAnJ2EE.dto.CourseProgressDto;
import Nhom100.DoAnJ2EE.dto.LessonProgressDto;
import Nhom100.DoAnJ2EE.entity.*;
import Nhom100.DoAnJ2EE.repository.*;
import Nhom100.DoAnJ2EE.service.CourseProgressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CourseProgressServiceImpl implements CourseProgressService {

    @Autowired
    private CourseProgressRepository courseProgressRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public CourseProgressDto getCourseProgress(Long userId, Long courseId) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null || course.getChapters() == null) {
            return new CourseProgressDto(courseId, 0, 0, 0.0, Collections.emptyList());
        }

        long totalLessons = course.getChapters().stream()
                .filter(ch -> ch.getLessons() != null)
                .flatMap(ch -> ch.getLessons().stream())
                .count();

        long completedLessons = courseProgressRepository.countCompletedLessons(userId, courseId);
        double percent = totalLessons > 0 ? (completedLessons * 100.0 / totalLessons) : 0.0;

        List<LessonProgressDto> lessonDtos = getLessonProgressList(userId, courseId);

        return new CourseProgressDto(courseId, totalLessons, completedLessons, percent, lessonDtos);
    }

    @Override
    @Transactional
    public void markLessonCompleted(Long userId, Long lessonId, Long courseId) {
        Optional<CourseProgress> existing = courseProgressRepository
                .findByUserIdAndLessonId(userId, lessonId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khóa học"));
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài giảng"));

        if (existing.isPresent()) {
            CourseProgress progress = existing.get();
            progress.setCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());
            courseProgressRepository.save(progress);
        } else {
            CourseProgress progress = new CourseProgress(user, course, lesson, true);
            progress.setCompletedAt(LocalDateTime.now());
            courseProgressRepository.save(progress);
        }
    }

    @Override
    @Transactional
    public void markLessonIncomplete(Long userId, Long lessonId) {
        Optional<CourseProgress> existing = courseProgressRepository
                .findByUserIdAndLessonId(userId, lessonId);
        existing.ifPresent(cp -> {
            cp.setCompleted(false);
            cp.setCompletedAt(null);
            courseProgressRepository.save(cp);
        });
    }

    @Override
    public long countCompletedLessons(Long userId, Long courseId) {
        return courseProgressRepository.countCompletedLessons(userId, courseId);
    }

    @Override
    public Map<Long, Boolean> getLessonCompletedMap(Long userId, Long courseId) {
        List<CourseProgress> progressList = courseProgressRepository
                .findByUserIdAndCourseId(userId, courseId);
        return progressList.stream()
                .collect(Collectors.toMap(
                        cp -> cp.getLesson().getId(),
                        CourseProgress::isCompleted
                ));
    }

    @Override
    public List<LessonProgressDto> getLessonProgressList(Long userId, Long courseId) {
        List<CourseProgress> progressList = courseProgressRepository
                .findByUserIdAndCourseId(userId, courseId);
        return progressList.stream()
                .map(cp -> new LessonProgressDto(
                        cp.getLesson().getId(),
                        cp.isCompleted(),
                        cp.getCompletedAt()
                ))
                .collect(Collectors.toList());
    }
}
