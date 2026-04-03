package Nhom100.DoAnJ2EE.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import Nhom100.DoAnJ2EE.service.CourseService;
import Nhom100.DoAnJ2EE.repository.CourseReviewRepository;
import Nhom100.DoAnJ2EE.entity.Course;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
@Controller
public class HomeController {
    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseReviewRepository courseReviewRepository;

    @GetMapping("/")
    public String home(Model model){

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = false;
        boolean isLogged = false;
        String userDisplayName = null;
        String userHandle = null;
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            isLogged = true;
            isAdmin = auth.getAuthorities().stream()
               .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            userDisplayName = auth.getName();
            userHandle = "@" + auth.getName();
        }
        
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isLogged", isLogged);
        model.addAttribute("userDisplayName", userDisplayName);
        model.addAttribute("userHandle", userHandle);

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

        return "homepage";
    }

}
