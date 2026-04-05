package Nhom100.DoAnJ2EE.controller;

import Nhom100.DoAnJ2EE.entity.Order;
import Nhom100.DoAnJ2EE.entity.User;
import Nhom100.DoAnJ2EE.repository.UserRepository;
import Nhom100.DoAnJ2EE.service.OrderService;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.ui.Model;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * VNPayController - Thanh toán qua cổng VNPay
 *
 * Flow:
 *  POST /vnpay/create?courseId={id}   → Tạo đơn → redirect sang trang VNPay
 *  GET  /vnpay/return                  → VNPay redirect về sau thanh toán
 */
@Controller
@RequestMapping("/vnpay")
public class VNPayController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private Nhom100.DoAnJ2EE.config.VNPayConfig vnPayConfig;

    // ── Helpers ──
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) return null;
        return userRepository.findByEmail(auth.getName()).orElse(null);
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

    /**
     * POST /vnpay/create
     * Tạo đơn hàng PENDING → build URL VNPay (HMAC-SHA512) → redirect
     */
    @PostMapping("/create")
    public String createPayment(
            @RequestParam Long courseId,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        try {
            User user = getCurrentUser();
            if (user == null) return "redirect:/login";

            String ipAddress = getClientIp(request);
            Map<String, Object> result = orderService.createVNPayOrder(user.getId(), courseId, ipAddress);

            Long orderId = (Long) result.get("orderId");
            String paymentCode = (String) result.get("paymentCode");
            Double amount = (Double) result.get("amount");

            // Số tiền: quy đổi USD → VND
            long amountVnd = vnPayConfig.convertToVnd(amount);

            // Build URL VNPay với HMAC-SHA512
            String paymentUrl = vnPayConfig.createPaymentUrl(
                    amountVnd,
                    paymentCode,
                    paymentCode,
                    ipAddress
            );

            System.err.println("[VNPay] Redirecting to: " + paymentUrl);

            // Redirect trình duyệt sang trang thanh toán VNPay
            return "redirect:" + paymentUrl;

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/courses";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            String cid = request.getParameter("courseId");
            return cid != null ? "redirect:/courses/" + cid : "redirect:/courses";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/courses";
        }
    }

    /**
     * GET /vnpay/return
     * VNPay redirect về sau khi thanh toán
     */
    @GetMapping("/return")
    public String vnpayReturn(
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        try {
            // Lấy toàn bộ params từ VNPay gửi về
            Map<String, String> params = Nhom100.DoAnJ2EE.config.VNPayConfig.getReturnFields(request);
            
            // Xử lý đơn hàng qua Service
            orderService.handleVNPayReturn(params);
            
            String responseCode = params.get("vnp_responsecode");
            String paymentCode  = params.get("vnp_orderinfo");

            // Redirect về trang kết quả thay vì quay về course detail trực tiếp
            return "redirect:/vnpay/result?paymentCode=" + paymentCode + "&responseCode=" + responseCode;

        } catch (Exception e) {
            System.err.println("[VNPay Return] Error: " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/courses";
        }
    }

    /**
     * GET /vnpay/result
     * Trang kết quả thanh toán hiển thị cho người dùng
     */
    @GetMapping("/result")
    public String paymentResult(
            @RequestParam String paymentCode,
            @RequestParam String responseCode,
            Model model
    ) {
        User user = getCurrentUser();
        addAuthAttributes(model, user);

        Order order = orderService.getOrderByPaymentCode(paymentCode);
        
        model.addAttribute("status", "00".equals(responseCode) ? "SUCCESS" : "FAILED");
        model.addAttribute("paymentCode", paymentCode);
        
        if (order != null) {
            model.addAttribute("amount", order.getTotalAmount());
            model.addAttribute("transactionId", order.getTransactionId());
            model.addAttribute("orderInfo", "Thanh toán khóa học");
        } else {
            model.addAttribute("amount", 0.0);
            model.addAttribute("orderInfo", "Không tìm thấy đơn hàng");
        }

        if (!"00".equals(responseCode)) {
            model.addAttribute("message", "Thanh toán không thành công (Mã lỗi: " + responseCode + ")");
        }

        return "order/payment-result";
    }

    /**
     * GET /vnpay/vnpay-ipn
     * Endpoint IPN (Backend-to-Backend) để VNPay gọi sang
     */
    @GetMapping("/vnpay-ipn")
    @ResponseBody
    public ResponseEntity<?> vnpayIPN(HttpServletRequest request) {
        try {
            Map<String, String> params = Nhom100.DoAnJ2EE.config.VNPayConfig.getReturnFields(request);
            
            // Log for debugging
            System.out.println("[VNPay IPN] Received notification for: " + params.get("vnp_txnref"));

            // Xác minh và cập nhật đơn hàng
            orderService.handleVNPayReturn(params);

            // Phản hồi VNPay theo chuẩn (JSON)
            Map<String, String> response = new java.util.HashMap<>();
            response.put("RspCode", "00");
            response.put("Message", "Confirm Success");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> response = new java.util.HashMap<>();
            response.put("RspCode", "99");
            response.put("Message", "Unknown error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
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
}
