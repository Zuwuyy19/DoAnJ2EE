package Nhom100.DoAnJ2EE.controller;

import Nhom100.DoAnJ2EE.entity.Cart;
import Nhom100.DoAnJ2EE.entity.Order;
import Nhom100.DoAnJ2EE.entity.User;
import Nhom100.DoAnJ2EE.repository.OrderRepository;
import Nhom100.DoAnJ2EE.repository.UserRepository;
import Nhom100.DoAnJ2EE.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/**
 * SimulatedPaymentController - Giả lập cổng thanh toán ATM cho demo
 *
 * Flow:
 *   GET  /payment/simulate/{paymentCode}   → Hiển thị trang nhập thẻ ATM
 *   POST /payment/simulate-confirm          → Xác nhận thanh toán → COMPLETED
 */
@Controller
@RequestMapping("/payment")
public class SimulatedPaymentController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartService cartService;

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) return null;
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }

    /**
     * GET /payment/simulate/{paymentCode}
     * Hiển thị trang giả lập cổng thanh toán ATM
     */
    @GetMapping("/simulate/{paymentCode}")
    public String showSimulatedPayment(@PathVariable String paymentCode, Model model) {
        User user = getAuthenticatedUser();
        if (user == null) return "redirect:/login";

        Optional<Order> orderOpt = orderRepository.findByPaymentCode(paymentCode);
        if (orderOpt.isEmpty()) {
            model.addAttribute("errorMessage", "Không tìm thấy đơn hàng với mã: " + paymentCode);
            return "payment/simulate";
        }

        Order order = orderOpt.get();

        // Kiểm tra đơn thuộc về user hiện tại
        if (!order.getUser().getId().equals(user.getId())) {
            model.addAttribute("errorMessage", "Bạn không có quyền thanh toán đơn hàng này.");
            return "payment/simulate";
        }

        // Nếu đã thanh toán rồi
        if ("COMPLETED".equals(order.getStatus())) {
            model.addAttribute("successMessage", "Đơn hàng đã được thanh toán thành công!");
            model.addAttribute("order", order);
            return "payment/simulate";
        }

        model.addAttribute("order", order);
        model.addAttribute("showForm", true);
        return "payment/simulate";
    }

    /**
     * POST /payment/simulate-confirm
     * Xác nhận thanh toán (giả lập) → cập nhật order COMPLETED → xóa cart
     */
    @PostMapping("/simulate-confirm")
    public String confirmPayment(@RequestParam String paymentCode,
                                 @RequestParam String cardNumber,
                                 @RequestParam String cardName,
                                 @RequestParam String cardExpiry,
                                 @RequestParam String otp,
                                 RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser();
        if (user == null) return "redirect:/login";

        Optional<Order> orderOpt = orderRepository.findByPaymentCode(paymentCode);
        if (orderOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đơn hàng không tồn tại.");
            return "redirect:/cart";
        }

        Order order = orderOpt.get();

        // Kiểm tra thẻ demo
        boolean isValidCard = "9704198526191432198".equals(cardNumber.replaceAll("\\s+", ""))
                && "NGUYEN VAN A".equalsIgnoreCase(cardName.trim())
                && "07/15".equals(cardExpiry.trim())
                && "123456".equals(otp.trim());

        if (!isValidCard) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Thông tin thẻ không hợp lệ! Vui lòng dùng thẻ demo: 9704198526191432198 / NGUYEN VAN A / 07/15 / OTP: 123456");
            return "redirect:/payment/simulate/" + paymentCode;
        }

        // Cập nhật trạng thái đơn hàng
        order.setStatus("COMPLETED");
        order.setPaymentType("ATM_DEMO");
        order.setTransactionId("DEMO-" + System.currentTimeMillis());
        orderRepository.save(order);

        // Xóa giỏ hàng
        Cart cart = cartService.getCartByUser(user);
        if (cart != null && cart.getItems() != null && !cart.getItems().isEmpty()) {
            cartService.clearCart(user);
        }

        redirectAttributes.addFlashAttribute("successMessage",
                "Thanh toán thành công! Bạn có thể xem khóa học trong Lịch sử mua hàng.");
        return "redirect:/orders/history";
    }

    // ═══════════════════════════════════════════════
    //  MOMO SIMULATED PAYMENT
    // ═══════════════════════════════════════════════

    /**
     * GET /payment/momo/{paymentCode}
     * Hiển thị trang giả lập thanh toán MoMo
     */
    @GetMapping("/momo/{paymentCode}")
    public String showMomoPayment(@PathVariable String paymentCode, Model model) {
        User user = getAuthenticatedUser();
        if (user == null) return "redirect:/login";

        Optional<Order> orderOpt = orderRepository.findByPaymentCode(paymentCode);
        if (orderOpt.isEmpty()) {
            model.addAttribute("errorMessage", "Không tìm thấy đơn hàng với mã: " + paymentCode);
            return "payment/momo";
        }

        Order order = orderOpt.get();

        if (!order.getUser().getId().equals(user.getId())) {
            model.addAttribute("errorMessage", "Bạn không có quyền thanh toán đơn hàng này.");
            return "payment/momo";
        }

        if ("COMPLETED".equals(order.getStatus())) {
            model.addAttribute("successMessage", "Đơn hàng đã được thanh toán thành công!");
            model.addAttribute("order", order);
            return "payment/momo";
        }

        // Tạo mã MoMo giả lập (6 số)
        String momoCode = String.valueOf(100000 + (int)(Math.random() * 900000));

        model.addAttribute("order", order);
        model.addAttribute("momoCode", momoCode);
        model.addAttribute("showForm", true);
        return "payment/momo";
    }

    /**
     * POST /payment/momo-confirm
     * Xác nhận thanh toán MoMo → COMPLETED
     */
    @PostMapping("/momo-confirm")
    public String confirmMomoPayment(@RequestParam String paymentCode,
                                     @RequestParam String momoPhone,
                                     RedirectAttributes redirectAttributes) {
        User user = getAuthenticatedUser();
        if (user == null) return "redirect:/login";

        Optional<Order> orderOpt = orderRepository.findByPaymentCode(paymentCode);
        if (orderOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đơn hàng không tồn tại.");
            return "redirect:/cart";
        }

        Order order = orderOpt.get();

        // Kiểm tra SĐT MoMo demo
        String phone = momoPhone != null ? momoPhone.replaceAll("\\s+", "") : "";
        if (phone.isEmpty() || phone.length() < 10) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Vui lòng nhập số điện thoại MoMo hợp lệ (ví dụ: 0912345678)");
            return "redirect:/payment/momo/" + paymentCode;
        }

        // Cập nhật trạng thái
        order.setStatus("COMPLETED");
        order.setPaymentType("MOMO");
        order.setTransactionId("MOMO-" + System.currentTimeMillis());
        orderRepository.save(order);

        // Xóa giỏ hàng
        cartService.clearCart(user);

        redirectAttributes.addFlashAttribute("successMessage",
                "Thanh toán MoMo thành công! Bạn có thể xem khóa học trong Lịch sử mua hàng.");
        return "redirect:/orders/history";
    }
}
