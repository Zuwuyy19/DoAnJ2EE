package Nhom100.DoAnJ2EE.controller;

import Nhom100.DoAnJ2EE.entity.Course;
import Nhom100.DoAnJ2EE.repository.CourseRepository;
import Nhom100.DoAnJ2EE.repository.CategoryRepository;
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
    
    private void addAuthAttributes(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = false;
        boolean isLogged = false;
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            isLogged = true;
            isAdmin = auth.getAuthorities().stream()
               .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        }
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isLogged", isLogged);
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
        if(course == null) return "redirect:/courses";
        
        model.addAttribute("course", course);
        return "course/detail";
    }
}
