package Nhom100.DoAnJ2EE.controller;

import Nhom100.DoAnJ2EE.entity.Course;
import Nhom100.DoAnJ2EE.entity.User;
import Nhom100.DoAnJ2EE.repository.CourseRepository;
import Nhom100.DoAnJ2EE.repository.CategoryRepository;
import Nhom100.DoAnJ2EE.repository.OrderDetailRepository;
import Nhom100.DoAnJ2EE.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Controller
public class CourseWebController {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private UserRepository userRepository;

    private Long getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal().toString())) {
            String email = auth.getName();
            User user = userRepository.findByEmail(email);
            return user != null ? user.getId() : null;
        }
        return null;
    }

    private void addAuthAttributes(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = false;
        boolean isLogged = false;
        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal().toString())) {
            isLogged = true;
            isAdmin = auth.getAuthorities().stream()
               .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        }
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isLogged", isLogged);
    }

    private boolean hasPurchased(Long userId, Long courseId) {
        if (userId == null) return false;
        return orderDetailRepository.existsByOrderUserIdAndCourseId(userId, courseId);
    }

    @GetMapping("/courses")
    public String listCourses(Model model) {
        addAuthAttributes(model);
        model.addAttribute("courses", courseRepository.findAll());
        model.addAttribute("categories", categoryRepository.findAll());
        return "course/list";
    }

    @GetMapping("/courses/{id}")
    public String courseDetail(@PathVariable Long id, Model model) {
        addAuthAttributes(model);

        Course course = courseRepository.findById(id).orElse(null);
        if (course == null) return "redirect:/courses";

        Long userId = getAuthenticatedUserId();
        boolean isPurchased = hasPurchased(userId, id);

        // Thống kê
        int totalChapters = course.getChapters() != null ? course.getChapters().size() : 0;
        int totalLessons = course.getChapters() == null ? 0
                : course.getChapters().stream()
                        .mapToInt(c -> c.getLessons() != null ? c.getLessons().size() : 0)
                        .sum();
        long totalStudents = orderDetailRepository.countDistinctStudentsByCourseId(id);

        // Khóa học liên quan (cùng danh mục, không bao gồm course hiện tại)
        var relatedCourses = course.getCategory() != null
                ? courseRepository.findByCategoryId(course.getCategory().getId())
                        .stream()
                        .filter(c -> !c.getId().equals(id))
                        .toList()
                : java.util.Collections.emptyList();

        model.addAttribute("course", course);
        model.addAttribute("isPurchased", isPurchased);
        model.addAttribute("totalChapters", totalChapters);
        model.addAttribute("totalLessons", totalLessons);
        model.addAttribute("totalStudents", totalStudents);
        model.addAttribute("relatedCourses", relatedCourses);

        return "course/detail";
    }
}
