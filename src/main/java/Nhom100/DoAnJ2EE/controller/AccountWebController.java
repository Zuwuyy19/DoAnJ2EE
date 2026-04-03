package Nhom100.DoAnJ2EE.controller;

import Nhom100.DoAnJ2EE.entity.User;
import Nhom100.DoAnJ2EE.service.UserService;
import Nhom100.DoAnJ2EE.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/account")
public class AccountWebController {

    @Autowired
    private UserService userService;

    @Autowired
    private OrderService orderService;

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            String email = auth.getName();
            return userService.getUserByEmail(email).orElse(null);
        }
        return null;
    }

    private void addAuthAttributes(Model model, User user) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = false;
        boolean isLogged = user != null;
        String userDisplayName = null;
        String userHandle = null;
        if (isLogged && user != null) {
            isAdmin = auth.getAuthorities().stream()
               .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            userDisplayName = user.getFullname() != null ? user.getFullname() : auth.getName();
            userHandle = "@" + auth.getName().split("@")[0];
        }
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isLogged", isLogged);
        model.addAttribute("userDisplayName", userDisplayName);
        model.addAttribute("userHandle", userHandle);
    }

    @GetMapping
    public String index(Model model) {
        User user = getAuthenticatedUser();
        if (user == null) {
            return "redirect:/login";
        }
        addAuthAttributes(model, user);
        if (user != null) {
            model.addAttribute("user", user);
            
            // Fetch stats
            model.addAttribute("purchasedCount", orderService.getMyCourses(user.getId()).size());
        }
        
        return "account/index";
    }

    @GetMapping("/edit")
    public String editForm(Model model) {
        User user = getAuthenticatedUser();
        if (user == null) {
            return "redirect:/login";
        }
        addAuthAttributes(model, user);
        model.addAttribute("user", user);
        return "account/edit";
    }

    @PostMapping("/edit")
    public String updateProfile(@ModelAttribute User updatedUser, RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser();
        if (user == null) {
            return "redirect:/login";
        }
        
        try {
            userService.updateProfile(user.getEmail(), updatedUser);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hồ sơ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        
        return "redirect:/account";
    }
}
