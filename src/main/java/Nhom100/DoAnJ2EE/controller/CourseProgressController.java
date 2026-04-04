package Nhom100.DoAnJ2EE.controller;

import Nhom100.DoAnJ2EE.dto.CourseProgressDto;
import Nhom100.DoAnJ2EE.dto.LessonProgressDto;
import Nhom100.DoAnJ2EE.entity.User;
import Nhom100.DoAnJ2EE.repository.OrderDetailRepository;
import Nhom100.DoAnJ2EE.repository.UserRepository;
import Nhom100.DoAnJ2EE.service.CourseProgressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API Controller cho chức năng theo dõi tiến độ học tập.
 * Base path: /api/progress
 * Tất cả endpoints đều yêu cầu JWT authentication (cấu hình trong SecurityConfig).
 */
@RestController
@RequestMapping("/api/progress")
public class CourseProgressController {

    @Autowired
    private CourseProgressService courseProgressService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    /**
     * Lấy user hiện tại từ SecurityContext (JWT authenticated).
     */
    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal().toString())) {
            return userRepository.findByEmail(auth.getName()).orElse(null);
        }
        return null;
    }

    /**
     * GET /api/progress/{courseId}
     * Lấy tổng tiến độ (% hoàn thành) của user trên một khóa học.
     */
    @GetMapping("/{courseId}")
    public ResponseEntity<?> getCourseProgress(@PathVariable Long courseId) {
        User user = getAuthenticatedUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đăng nhập"));
        }

        boolean hasPurchased = orderDetailRepository
                .existsCompletedOrderByUserAndCourse(user.getId(), courseId);
        if (!hasPurchased) {
            return ResponseEntity.status(403).body(Map.of("error", "Bạn chưa mua khóa học này"));
        }

        CourseProgressDto progress = courseProgressService.getCourseProgress(user.getId(), courseId);
        return ResponseEntity.ok(progress);
    }

    /**
     * POST /api/progress/mark
     * Đánh dấu một bài giảng là đã hoàn thành.
     * Body: { lessonId, courseId }
     */
    @PostMapping("/mark")
    public ResponseEntity<?> markLessonCompleted(
            @RequestParam Long lessonId,
            @RequestParam Long courseId) {

        User user = getAuthenticatedUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đăng nhập"));
        }

        boolean hasPurchased = orderDetailRepository
                .existsCompletedOrderByUserAndCourse(user.getId(), courseId);
        if (!hasPurchased) {
            return ResponseEntity.status(403).body(Map.of("error", "Bạn chưa mua khóa học này"));
        }

        courseProgressService.markLessonCompleted(user.getId(), lessonId, courseId);

        CourseProgressDto updatedProgress = courseProgressService.getCourseProgress(user.getId(), courseId);
        return ResponseEntity.ok(updatedProgress);
    }

    /**
     * DELETE /api/progress/{courseId}/{lessonId}
     * Bỏ đánh dấu hoàn thành một bài giảng.
     */
    @DeleteMapping("/{courseId}/{lessonId}")
    public ResponseEntity<?> markLessonIncomplete(
            @PathVariable Long courseId,
            @PathVariable Long lessonId) {

        User user = getAuthenticatedUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đăng nhập"));
        }

        courseProgressService.markLessonIncomplete(user.getId(), lessonId);

        CourseProgressDto updatedProgress = courseProgressService.getCourseProgress(user.getId(), courseId);
        return ResponseEntity.ok(updatedProgress);
    }

    /**
     * GET /api/progress/{courseId}/lessons
     * Lấy danh sách LessonProgressDto cho sidebar.
     */
    @GetMapping("/{courseId}/lessons")
    public ResponseEntity<?> getLessonProgress(@PathVariable Long courseId) {
        User user = getAuthenticatedUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đăng nhập"));
        }

        Map<Long, Boolean> completedMap = courseProgressService.getLessonCompletedMap(user.getId(), courseId);
        return ResponseEntity.ok(completedMap);
    }
}
