package Nhom100.DoAnJ2EE.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import Nhom100.DoAnJ2EE.service.CourseService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
@Controller
public class HomeController {
    @Autowired
    private CourseService courseService;

    @GetMapping("/")
    public String home(Model model){

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

        model.addAttribute("courses", courseService.getAllCourses());

        return "homepage";
    }

}
