package Nhom100.DoAnJ2EE.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import Nhom100.DoAnJ2EE.service.CourseService;
@Controller
public class HomeController {
    @Autowired
    private CourseService courseService;

    @GetMapping("/")
    public String home(Model model){

        model.addAttribute("courses", courseService.getAllCourses());

        return "homepage";
    }

}
