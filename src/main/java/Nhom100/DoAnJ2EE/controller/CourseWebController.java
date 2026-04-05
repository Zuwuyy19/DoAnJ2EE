package Nhom100.DoAnJ2EE.controller;

import Nhom100.DoAnJ2EE.entity.Course;
import Nhom100.DoAnJ2EE.entity.CourseReview;
import Nhom100.DoAnJ2EE.entity.User;
import Nhom100.DoAnJ2EE.repository.CourseRepository;
import Nhom100.DoAnJ2EE.repository.CourseReviewRepository;
import Nhom100.DoAnJ2EE.repository.CategoryRepository;
import Nhom100.DoAnJ2EE.repository.OrderDetailRepository;
import Nhom100.DoAnJ2EE.repository.UserRepository;
import Nhom100.DoAnJ2EE.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class CourseWebController {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseReviewRepository courseReviewRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartService cartService;

    private Long getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal().toString())) {
            String email = auth.getName();
            User user = userRepository.findByEmail(email).orElse(null);
            return user != null ? user.getId() : null;
        }
        return null;
    }

    private void addAuthAttributes(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = false;
        boolean isLogged = false;
        String userDisplayName = null;
        String userHandle = null;
        User currentUser = null;

        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal().toString())) {
            isLogged = true;
            isAdmin = auth.getAuthorities().stream()
               .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            userDisplayName = auth.getName();
            userHandle = "@" + auth.getName();
            currentUser = userRepository.findByEmail(auth.getName()).orElse(null);
        }

        int cartItemCount = (currentUser != null) ? cartService.getCartItemCount(currentUser) : 0;

        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isLogged", isLogged);
        model.addAttribute("userDisplayName", userDisplayName);
        model.addAttribute("userHandle", userHandle);
        model.addAttribute("cartItemCount", cartItemCount);
    }

    private boolean hasPurchased(Long userId, Long courseId) {
        if (userId == null) return false;
        return orderDetailRepository.existsByOrderUserIdAndCourseId(userId, courseId);
    }

    @GetMapping("/courses")
    public String listCourses(Model model,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) String q) {
        addAuthAttributes(model);
        List<Course> courses = categoryId != null
                ? courseRepository.findByCategoryId(categoryId)
                : courseRepository.findAll();

        Map<Long, Double> courseAvgRatings = new HashMap<>();
        Map<Long, Long> courseReviewCounts = new HashMap<>();
        for (CourseReviewRepository.CourseRatingSummary s : courseReviewRepository.findCourseRatings()) {
            courseAvgRatings.put(s.getCourseId(), s.getAvgRating());
            courseReviewCounts.put(s.getCourseId(), s.getTotalReviews());
        }

        String qNorm = q != null ? q.trim().toLowerCase() : null;
        courses = courses.stream()
                .filter(c -> {
                    if (qNorm == null || qNorm.isEmpty()) return true;
                    String title = c.getTitle() != null ? c.getTitle().toLowerCase() : "";
                    String name = c.getName() != null ? c.getName().toLowerCase() : "";
                    String desc = c.getDescription() != null ? c.getDescription().toLowerCase() : "";
                    return title.contains(qNorm) || name.contains(qNorm) || desc.contains(qNorm);
                })
                .filter(c -> {
                    if (minRating == null) return true;
                    Double avg = courseAvgRatings.get(c.getId());
                    if (avg == null) return false;
                    return avg >= minRating;
                })
                .sorted((a, b) -> {
                    Double ra = courseAvgRatings.get(a.getId());
                    Double rb = courseAvgRatings.get(b.getId());
                    double va = ra != null ? ra : -1;
                    double vb = rb != null ? rb : -1;
                    return Double.compare(vb, va);
                })
                .collect(Collectors.toList());

        model.addAttribute("courses", courses);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("courseAvgRatings", courseAvgRatings);
        model.addAttribute("courseReviewCounts", courseReviewCounts);

        // purchasedCourseIds cho trang danh sách
        Set<Long> purchasedCourseIds = new HashSet<>();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            User user = userRepository.findByEmail(auth.getName()).orElse(null);
            if (user != null) {
                purchasedCourseIds = orderDetailRepository.findByOrderUserId(user.getId()).stream()
                        .filter(od -> od.getOrder() != null && ("COMPLETED".equals(od.getOrder().getStatus()) || "PAID".equals(od.getOrder().getStatus())))
                        .map(od -> od.getCourse().getId())
                        .collect(Collectors.toSet());
            }
        }
        model.addAttribute("purchasedCourseIds", purchasedCourseIds);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("minRating", minRating);
        model.addAttribute("q", q);
        return "course/list";
    }

    @GetMapping("/courses/{id}")
    public String courseDetail(@PathVariable Long id, Model model) {
        addAuthAttributes(model);

        Course course = courseRepository.findById(id).orElse(null);
        if (course == null) return "redirect:/courses";

        Long userId = getAuthenticatedUserId();
        boolean isPurchased = hasPurchased(userId, id);
        CourseReview existingReview = null;
        if (userId != null) {
            existingReview = courseReviewRepository.findByCourseIdAndUserId(id, userId).orElse(null);
        }

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
        model.addAttribute("courseReviews", courseReviewRepository.findByCourseIdOrderByCreatedAtDesc(id));
        model.addAttribute("existingReview", existingReview);

        return "course/detail";
    }

    @PostMapping("/courses/{id}/reviews")
    public String submitReview(@PathVariable Long id,
            @RequestParam Integer rating,
            @RequestParam String comment,
            RedirectAttributes redirectAttributes) {
        Long userId = getAuthenticatedUserId();
        if (userId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng đăng nhập để đánh giá khóa học.");
            return "redirect:/login";
        }
        if (!hasPurchased(userId, id)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn chỉ có thể đánh giá sau khi mua khóa học.");
            return "redirect:/courses/" + id;
        }

        if (rating == null || rating < 1 || rating > 5 || comment == null || comment.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng nhập điểm đánh giá (1-5) và bình luận.");
            return "redirect:/courses/" + id;
        }

        Course course = courseRepository.findById(id).orElse(null);
        User user = userRepository.findById(userId).orElse(null);
        if (course == null || user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy khóa học hoặc người dùng.");
            return "redirect:/courses";
        }

        CourseReview review = courseReviewRepository.findByCourseIdAndUserId(id, userId).orElse(null);
        if (review == null) {
            review = new CourseReview();
            review.setCourse(course);
            review.setUser(user);
        }
        review.setRating(rating);
        review.setComment(comment.trim());
        courseReviewRepository.save(review);

        redirectAttributes.addFlashAttribute("successMessage", "Đã lưu đánh giá của bạn.");
        return "redirect:/courses/" + id;
    }
}
