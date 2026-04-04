package Nhom100.DoAnJ2EE.service;

import Nhom100.DoAnJ2EE.dto.OrderResponse;
import Nhom100.DoAnJ2EE.entity.Cart;
import Nhom100.DoAnJ2EE.entity.User;
import java.util.List;
import java.util.Map;

public interface CartService {
    Cart getCartByUser(User user);
    Cart addToCart(User user, Long courseId);
    int getCartItemCount(User user);
    Cart removeFromCart(User user, Long cartItemId);
    List<OrderResponse> checkout(User user);

    /**
     * Xóa toàn bộ item trong giỏ hàng của user
     */
    void clearCart(User user);

    /**
     * Tạo đơn hàng VNPay cho toàn bộ giỏ hàng
     * @return Map chứa orderId, paymentCode, paymentUrl, amount
     */
    Map<String, Object> checkoutVNPay(User user, String ipAddress);
}
