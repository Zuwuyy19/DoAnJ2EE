package Nhom100.DoAnJ2EE.service;

import Nhom100.DoAnJ2EE.dto.OrderResponse;
import Nhom100.DoAnJ2EE.entity.Cart;
import Nhom100.DoAnJ2EE.entity.User;
import java.util.List;

public interface CartService {
    Cart getCartByUser(User user);
    Cart addToCart(User user, Long courseId);
    Cart removeFromCart(User user, Long cartItemId);
    List<OrderResponse> checkout(User user);
    int getCartItemCount(User user);
}
