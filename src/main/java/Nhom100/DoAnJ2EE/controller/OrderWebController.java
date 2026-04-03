package Nhom100.DoAnJ2EE.controller;

import Nhom100.DoAnJ2EE.dto.OrderResponse;
import Nhom100.DoAnJ2EE.entity.User;
import Nhom100.DoAnJ2EE.repository.UserRepository;
import Nhom100.DoAnJ2EE.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/orders")
public class OrderWebController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            String email = auth.getName();
            return userRepository.findByEmail(email);
        }
        return null;
    }

    private void addAuthAttributes(Model model, User user) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = false;
        boolean isLogged = user != null;
        String userDisplayName = null;
        String userHandle = null;
        if (isLogged) {
            isAdmin = auth.getAuthorities().stream()
               .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            userDisplayName = auth.getName();
            userHandle = "@" + auth.getName();
        }
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isLogged", isLogged);
        model.addAttribute("userDisplayName", userDisplayName);
        model.addAttribute("userHandle", userHandle);
    }

    @GetMapping("/history")
    public String viewOrderHistory(Model model) {
        User user = getAuthenticatedUser();
        if (user == null) {
            return "redirect:/login";
        }
        addAuthAttributes(model, user);

        List<OrderResponse> orders = orderService.getMyCourses(user.getId());
        model.addAttribute("orders", orders);
        
        return "order/history";
    }
}
