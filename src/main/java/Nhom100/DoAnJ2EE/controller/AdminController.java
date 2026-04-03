package Nhom100.DoAnJ2EE.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import Nhom100.DoAnJ2EE.entity.Course;
import Nhom100.DoAnJ2EE.entity.Category;
import Nhom100.DoAnJ2EE.entity.User;
import Nhom100.DoAnJ2EE.service.CourseService;
import Nhom100.DoAnJ2EE.service.UserService;
import Nhom100.DoAnJ2EE.repository.CategoryRepository;
import Nhom100.DoAnJ2EE.repository.UserRepository;
import Nhom100.DoAnJ2EE.repository.RoleRepository;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private RoleRepository roleRepository;

    @GetMapping
    public String adminDashboard(Model model) {
        model.addAttribute("courseCount", courseService.getAllCourses().size());
        model.addAttribute("categoryCount", categoryRepository.count());
        model.addAttribute("userCount", userRepository.count());
        return "admin/admin";
    }

    // --- QUẢN LÝ KHÓA HỌC ---

    @GetMapping("/courses")
    public String listCourses(Model model) {
        model.addAttribute("courses", courseService.getAllCourses());
        return "admin/course/list";
    }

    @GetMapping("/courses/new")
    public String showAddForm(Model model) {
        model.addAttribute("course", new Course());
        model.addAttribute("categories", categoryRepository.findAll());
        return "admin/course/form";
    }

    @GetMapping("/courses/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Course course = courseService.getCourseById(id).orElse(null);
        if (course == null) return "redirect:/admin/courses";
        model.addAttribute("course", course);
        model.addAttribute("categories", categoryRepository.findAll());
        return "admin/course/form";
    }

    @PostMapping("/courses/save")
    public String saveCourse(@ModelAttribute("course") Course course) {
        courseService.saveCourse(course);
        return "redirect:/admin/courses";
    }

    @GetMapping("/courses/delete/{id}")
    public String deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return "redirect:/admin/courses";
    }

    // --- QUẢN LÝ DANH MỤC ---

    @GetMapping("/categories")
    public String listCategories(Model model) {
        model.addAttribute("categories", categoryRepository.findAll());
        return "admin/category/list";
    }

    @GetMapping("/categories/new")
    public String showAddCategoryForm(Model model) {
        model.addAttribute("category", new Category());
        return "admin/category/form";
    }

    @GetMapping("/categories/edit/{id}")
    public String showEditCategoryForm(@PathVariable Long id, Model model) {
        Category category = categoryRepository.findById(id).orElse(null);
        if (category == null) return "redirect:/admin/categories";
        model.addAttribute("category", category);
        return "admin/category/form";
    }

    @PostMapping("/categories/save")
    public String saveCategory(@ModelAttribute("category") Category category) {
        categoryRepository.save(category);
        return "redirect:/admin/categories";
    }

    @GetMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable Long id) {
        // Lưu ý: Nếu danh mục có khóa học, hibernate sẽ báo lỗi hoặc xóa cascade tùy config
        categoryRepository.deleteById(id);
        return "redirect:/admin/categories";
    }

    // --- QUẢN LÝ NGƯỜI DÙNG ---

    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        return "admin/user/list";
    }

    @GetMapping("/users/edit/{id}")
    public String showEditUserForm(@PathVariable Long id, Model model) {
        User user = userService.getUserById(id).orElse(null);
        if (user == null) return "redirect:/admin/users";
        model.addAttribute("user", user);
        model.addAttribute("allRoles", roleRepository.findAll());
        return "admin/user/form";
    }

    @PostMapping("/users/save")
    public String saveUser(@ModelAttribute("user") User user) {
        // Lấy user cũ từ DB để giữ lại password (không thay đổi password ở đây)
        User existingUser = userService.getUserById(user.getId()).orElse(null);
        if (existingUser != null) {
            user.setPassword(existingUser.getPassword());
            user.setEmail(existingUser.getEmail()); // Giữ email không đổi nếu cần
            userService.saveUser(user);
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return "redirect:/admin/users";
    }
}
