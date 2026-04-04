package Nhom100.DoAnJ2EE.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import Nhom100.DoAnJ2EE.service.CourseService;
import Nhom100.DoAnJ2EE.service.CartService;
import Nhom100.DoAnJ2EE.repository.CourseReviewRepository;
import Nhom100.DoAnJ2EE.repository.OrderDetailRepository;
import Nhom100.DoAnJ2EE.repository.UserRepository;
import Nhom100.DoAnJ2EE.entity.Course;
import Nhom100.DoAnJ2EE.entity.User;
import Nhom100.DoAnJ2EE.entity.OrderDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
@Controller
public class HomeController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseReviewRepository courseReviewRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/")
    public String home(Model model) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = false;
        boolean isLogged = false;
        String userDisplayName = null;
        String userHandle = null;
        User currentUser = null;

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            isLogged = true;
            isAdmin = auth.getAuthorities().stream()
               .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            userDisplayName = auth.getName();
            userHandle = "@" + auth.getName();
            currentUser = userRepository.findByEmail(auth.getName()).orElse(null);
        }

        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isLogged", isLogged);
        model.addAttribute("userDisplayName", userDisplayName);
        model.addAttribute("userHandle", userHandle);

        // Cart item count cho navbar
        int cartItemCount = 0;
        if (currentUser != null) {
            cartItemCount = cartService.getCartItemCount(currentUser);
        }
        model.addAttribute("cartItemCount", cartItemCount);

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

        // Khóa học đã mua (COMPLETED)
        Set<Long> purchasedCourseIds = new HashSet<>();
        if (currentUser != null) {
            List<OrderDetail> purchased = orderDetailRepository.findByOrderUserId(currentUser.getId());
            purchasedCourseIds = purchased.stream()
                    .filter(od -> od.getOrder() != null && "COMPLETED".equals(od.getOrder().getStatus()))
                    .map(od -> od.getCourse().getId())
                    .collect(Collectors.toSet());
        }
        model.addAttribute("purchasedCourseIds", purchasedCourseIds);

        return "homepage";
    }
}
