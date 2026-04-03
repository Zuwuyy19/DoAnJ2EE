package Nhom100.DoAnJ2EE.controller;

import Nhom100.DoAnJ2EE.entity.Cart;
import Nhom100.DoAnJ2EE.entity.User;
import Nhom100.DoAnJ2EE.repository.UserRepository;
import Nhom100.DoAnJ2EE.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cart")
public class CartWebController {

    @Autowired
    private CartService cartService;

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

    @GetMapping
    public String viewCart(Model model) {
        User user = getAuthenticatedUser();
        if (user == null) {
            return "redirect:/login";
        }
        addAuthAttributes(model, user);
        
        Cart cart = cartService.getCartByUser(user);
        model.addAttribute("cart", cart);
        return "cart/index";
    }

    @PostMapping("/add/{courseId}")
    public String addToCart(@PathVariable Long courseId, RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser();
        if (user == null) {
            return "redirect:/login";
        }

        try {
            cartService.addToCart(user, courseId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm khóa học vào giỏ hàng");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        
        return "redirect:/courses/" + courseId;
    }

    @PostMapping("/remove/{itemId}")
    public String removeFromCart(@PathVariable Long itemId) {
        User user = getAuthenticatedUser();
        if (user == null) {
            return "redirect:/login";
        }

        cartService.removeFromCart(user, itemId);
        return "redirect:/cart";
    }

    @PostMapping("/checkout")
    public String checkout(RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser();
        if (user == null) {
            return "redirect:/login";
        }

        try {
            cartService.checkout(user);
            redirectAttributes.addFlashAttribute("successMessage", "Thanh toán thành công! Bạn có thể xem khóa học trong Lịch sử mua hàng.");
            return "redirect:/orders/history";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/cart";
        }
    }
}
