package Nhom100.DoAnJ2EE.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import Nhom100.DoAnJ2EE.service.CourseService;
import Nhom100.DoAnJ2EE.repository.CourseReviewRepository;
import Nhom100.DoAnJ2EE.entity.Course;
import Nhom100.DoAnJ2EE.entity.Category;
import Nhom100.DoAnJ2EE.repository.CategoryRepository;
import Nhom100.DoAnJ2EE.service.CartService;
import Nhom100.DoAnJ2EE.entity.User;
import Nhom100.DoAnJ2EE.repository.UserRepository;
import Nhom100.DoAnJ2EE.repository.OrderDetailRepository;
import Nhom100.DoAnJ2EE.entity.OrderDetail;
import java.util.Set;
import java.util.HashSet;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.time.LocalDateTime;
@Controller
public class HomeController {
    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseReviewRepository courseReviewRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @GetMapping("/")
    public String home(Model model){
        ensureDefaultCategories();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = false;
        boolean isLogged = false;
        String userDisplayName = null;
        String userHandle = null;
        int cartItemCount = 0;
        Set<Long> purchasedCourseIds = new HashSet<>();
        Map<Long, LocalDateTime> purchasedCourseDates = new HashMap<>();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            isLogged = true;
            isAdmin = auth.getAuthorities().stream()
               .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            userDisplayName = auth.getName();
            userHandle = "@" + auth.getName();
            User user = userRepository.findByEmail(auth.getName()).orElse(null);
            if (user != null) {
                cartItemCount = cartService.getCartItemCount(user);
                List<OrderDetail> orderDetails = orderDetailRepository.findByOrderUserId(user.getId());
                for (OrderDetail od : orderDetails) {
                    if (od.getCourse() != null) {
                        Long courseId = od.getCourse().getId();
                        purchasedCourseIds.add(courseId);
                        if (od.getOrder() != null) {
                            purchasedCourseDates.put(courseId, od.getOrder().getOrderDate());
                        }
                    }
                }
            }
        }
        
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isLogged", isLogged);
        model.addAttribute("userDisplayName", userDisplayName);
        model.addAttribute("userHandle", userHandle);
        model.addAttribute("cartItemCount", cartItemCount);
        model.addAttribute("purchasedCourseIds", purchasedCourseIds);
        model.addAttribute("purchasedCourseDates", purchasedCourseDates);

        Map<Long, Double> courseAvgRatings = new HashMap<>();
        Map<Long, Long> courseReviewCounts = new HashMap<>();
        for (CourseReviewRepository.CourseRatingSummary s : courseReviewRepository.findCourseRatings()) {
            courseAvgRatings.put(s.getCourseId(), s.getAvgRating());
            courseReviewCounts.put(s.getCourseId(), s.getTotalReviews());
        }
        model.addAttribute("courseAvgRatings", courseAvgRatings);
        model.addAttribute("courseReviewCounts", courseReviewCounts);

        List<Course> allCourses = courseService.getAllCourses();
        allCourses.sort((a, b) -> {
            Double ra = courseAvgRatings.get(a.getId());
            Double rb = courseAvgRatings.get(b.getId());
            double va = ra != null ? ra : -1;
            double vb = rb != null ? rb : -1;
            return Double.compare(vb, va);
        });
        model.addAttribute("courses", allCourses.size() > 6 ? allCourses.subList(0, 6) : allCourses);
        List<Category> categories = categoryRepository.findAll();
        model.addAttribute("categories", categories.size() > 4 ? categories.subList(0, 4) : categories);

        return "homepage";
    }

    private void ensureDefaultCategories() {
        String[] defaults = new String[] { "Lập trình Java", "Web Front-End", "An ninh mạng", "Kỹ năng mềm" };
        for (String name : defaults) {
            if (!categoryRepository.existsByName(name)) {
                Category category = new Category();
                category.setName(name);
                categoryRepository.save(category);
            }
        }
    }

}
