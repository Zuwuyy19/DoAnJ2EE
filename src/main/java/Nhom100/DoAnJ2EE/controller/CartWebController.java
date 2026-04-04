package Nhom100.DoAnJ2EE.controller;

import Nhom100.DoAnJ2EE.config.VNPayConfig;
import Nhom100.DoAnJ2EE.entity.Cart;
import Nhom100.DoAnJ2EE.entity.User;
import Nhom100.DoAnJ2EE.repository.UserRepository;
import Nhom100.DoAnJ2EE.service.CartService;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

/**
 * CartWebController - Quản lý giỏ hàng
 */
@Controller
@RequestMapping("/cart")
public class CartWebController {

    @Autowired private CartService cartService;
    @Autowired private UserRepository userRepository;
    @Autowired private VNPayConfig vnPayConfig;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) return null;
        return userRepository.findByEmail(auth.getName());
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

    // ── Trang giỏ hàng ──
    @GetMapping
    public String viewCart(Model model) {
        User user = getCurrentUser();
        if (user == null) return "redirect:/login";
        addAuthAttributes(model, user);
        Cart cart = cartService.getCartByUser(user);
        model.addAttribute("cart", cart);
        return "cart/index";
    }

    // ── Thêm vào giỏ hàng (AJAX) ──
    @PostMapping("/add/{courseId}")
    @ResponseBody
    public ResponseEntity<?> addToCartAjax(@PathVariable Long courseId) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("success", false, "message", "Vui lòng đăng nhập"));
        }
        try {
            Cart cart = cartService.addToCart(user, courseId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã thêm vào giỏ hàng!",
                    "cartCount", cart.getItems().size()
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Lỗi: " + e.getMessage()));
        }
    }

    // ── Thêm vào giỏ hàng (redirect cũ - fallback) ──
    @PostMapping("/add")
    public String addToCartFallback(@RequestParam Long courseId,
                                    RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        if (user == null) return "redirect:/login";
        try {
            cartService.addToCart(user, courseId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm khóa học vào giỏ hàng");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/courses/" + courseId;
    }

    // ── Xóa khỏi giỏ hàng ──
    @PostMapping("/remove/{itemId}")
    public String removeFromCart(@PathVariable Long itemId) {
        User user = getCurrentUser();
        if (user == null) return "redirect:/login";
        cartService.removeFromCart(user, itemId);
        return "redirect:/cart";
    }

    // ── Thanh toán Demo (fake) ──
    @PostMapping("/checkout")
    public String checkout(RedirectAttributes redirectAttributes) {
        User user = getCurrentUser();
        if (user == null) return "redirect:/login";
        try {
            cartService.checkout(user);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Thanh toán thành công!");
            return "redirect:/orders/history";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/cart";
        }
    }

    // ── Thanh toán VNPay - redirect trực tiếp sang sandbox ──
    @PostMapping("/checkout-vnpay")
    public String checkoutVNPay(RedirectAttributes redirectAttributes,
                                HttpServletRequest request) {
        User user = getCurrentUser();
        if (user == null) return "redirect:/login";
        try {
            String ipAddress = getClientIp(request);
            Map<String, Object> result = cartService.checkoutVNPay(user, ipAddress);

            String paymentCode = (String) result.get("paymentCode");
            Double amount = (Double) result.get("amount");
            // Giá đã lưu trực tiếp là VND
            long amountVnd = vnPayConfig.convertToVnd(amount);

            // Build URL VNPay với HMAC-SHA512
            String paymentUrl = vnPayConfig.createPaymentUrl(amountVnd, paymentCode, paymentCode, ipAddress);

            return "redirect:" + paymentUrl;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/cart";
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip))
            ip = request.getHeader("Proxy-Client-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip))
            ip = request.getHeader("WL-Proxy-Client-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip))
            ip = request.getRemoteAddr();
        if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
        return ip;
    }
}
