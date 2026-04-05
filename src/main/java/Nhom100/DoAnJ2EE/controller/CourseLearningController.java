package Nhom100.DoAnJ2EE.controller;

import Nhom100.DoAnJ2EE.entity.Course;
import Nhom100.DoAnJ2EE.entity.User;
import Nhom100.DoAnJ2EE.repository.CourseRepository;
import Nhom100.DoAnJ2EE.repository.OrderDetailRepository;
import Nhom100.DoAnJ2EE.repository.UserRepository;
import Nhom100.DoAnJ2EE.service.CourseProgressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

/**
 * Controller cho trang học trực tuyến (/learn/{courseId}).
 * Chỉ user đã mua khóa học (hoặc admin) mới được truy cập.
 */
@Controller
public class CourseLearningController extends BaseController {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private CourseProgressService courseProgressService;

    /**
     * Lấy thông tin xác thực cho navbar/header.
     */
    private void addAuthAttributes(Model model) {
        User user = getCurrentUser();
        boolean isLogged = user != null;
        boolean isAdmin = false;
        String userDisplayName = null;
        String userHandle = null;
        if (isLogged) {
            isAdmin = SecurityContextHolder.getContext().getAuthentication()
                    .getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            userDisplayName = user.getEmail();
            userHandle = "@" + user.getEmail();
        }
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isLogged", isLogged);
        model.addAttribute("userDisplayName", userDisplayName);
        model.addAttribute("userHandle", userHandle);
        model.addAttribute("cartItemCount", getCartItemCount());
    }

    /**
     * GET /learn/{courseId}
     * Trang học chính — hiển thị video player + sidebar chapters/lessons + tài
     * liệu.
     * Kiểm tra quyền: user đã mua khóa học hoặc là admin.
     */
    @GetMapping("/learn/{courseId}")
    public String learningPage(@PathVariable Long courseId,
            @RequestParam(required = false) Long lessonId,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Bước 1: Lấy user hiện tại
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/login";
        }

        // Bước 2: Load khóa học
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy khóa học.");
            return "redirect:/courses";
        }

        // Bước 3: Kiểm tra quyền
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        boolean hasPurchased = orderDetailRepository
                .existsCompletedOrderByUserAndCourse(user.getId(), courseId);

        if (!hasPurchased && !isAdmin) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Bạn cần mua khóa học này để truy cập trang học.");
            return "redirect:/courses/" + courseId;
        }

        // Bước 4: Thêm auth attributes cho navbar
        addAuthAttributes(model);

        // Bước 5: Tính tiến độ học tập
        Map<Long, Boolean> lessonCompletedMap = courseProgressService
                .getLessonCompletedMap(user.getId(), courseId);

        long completedLessons = courseProgressService.countCompletedLessons(user.getId(), courseId);

        // Tính tổng số bài
        long totalLessons = course.getChapters() == null ? 0
                : course.getChapters().stream()
                        .filter(ch -> ch.getLessons() != null)
                        .flatMap(ch -> ch.getLessons().stream())
                        .count();

        double percentComplete = totalLessons > 0
                ? (completedLessons * 100.0 / totalLessons)
                : 0.0;

        // Bước 6: Đẩy dữ liệu ra model
        model.addAttribute("course", course);
        model.addAttribute("lessonCompletedMap", lessonCompletedMap);
        model.addAttribute("totalLessons", totalLessons);
        model.addAttribute("completedLessons", completedLessons);
        model.addAttribute("percentComplete", percentComplete);
        model.addAttribute("initialLessonId", lessonId);

        return "learn/index";
    }
}
