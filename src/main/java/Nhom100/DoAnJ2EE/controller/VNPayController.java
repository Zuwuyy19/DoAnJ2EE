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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        return userRepository.findByEmail(auth.getName());
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

            // Debug: log TẤT CẢ params VNPay gửi về
            System.err.println("[VNPay Return] ===== RAW PARAMS FROM VNPAY =====");
            for (Map.Entry<String, String> e : params.entrySet()) {
                System.err.println("  " + e.getKey() + " = " + e.getValue());
            }

            // Xác minh hash — dùng copy vì verifyReturn sẽ xóa hash keys khỏi map
            boolean isValid = vnPayConfig.verifyReturn(new java.util.HashMap<>(params));
            System.err.println("[VNPay Return] Hash valid: " + isValid);

            // Đọc params — keys đã được chuyển lowercase trong getReturnFields
            String responseCode = params.get("vnp_responsecode");
            String orderInfo    = params.get("vnp_orderinfo");
            String txnRef       = params.get("vnp_txnref");

            System.err.println("[VNPay Return] ResponseCode=" + responseCode
                    + ", OrderInfo=" + orderInfo + ", TxnRef=" + txnRef);

            if (orderInfo == null || orderInfo.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Không nhận được mã thanh toán từ VNPay!");
                return "redirect:/courses";
            }

            // Cập nhật trạng thái đơn hàng
            Order order = orderService.handleVNPayReturn(params);

            if ("00".equals(responseCode)) {
                if (order != null && order.getOrderDetails() != null
                        && !order.getOrderDetails().isEmpty()) {
                    Long courseId = order.getOrderDetails().get(0).getCourse().getId();
                    redirectAttributes.addFlashAttribute("successMessage",
                            "Thanh toán thành công! Bạn đã có quyền truy cập khóa học.");
                    return "redirect:/courses/" + courseId;
                }
                redirectAttributes.addFlashAttribute("successMessage",
                        "Thanh toán thành công!");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Thanh toán thất bại hoặc bị hủy (Mã: " + responseCode + ")");
            }

        } catch (Exception e) {
            System.err.println("[VNPay Return] Error: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }

        return "redirect:/courses";
    }
}
