package Nhom100.DoAnJ2EE.controller;

import Nhom100.DoAnJ2EE.entity.User;
import Nhom100.DoAnJ2EE.repository.UserRepository;
import Nhom100.DoAnJ2EE.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Base controller — cung cấp helper auth + cart count cho tất cả controller con.
 */
public abstract class BaseController {

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected CartService cartService;

    /**
     * Trả về User hiện tại hoặc null nếu chưa login.
     */
    protected User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal().toString())) {
            return null;
        }
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }

    /**
     * Đếm số item trong giỏ hàng của user hiện tại (0 nếu chưa login).
     */
    protected int getCartItemCount() {
        User user = getCurrentUser();
        if (user == null) return 0;
        return cartService.getCartItemCount(user);
    }
}
