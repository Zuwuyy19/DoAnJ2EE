package Nhom100.DoAnJ2EE.service.impl;

import Nhom100.DoAnJ2EE.dto.OrderResponse;
import Nhom100.DoAnJ2EE.entity.*;
import Nhom100.DoAnJ2EE.repository.*;
import Nhom100.DoAnJ2EE.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Override
    public Cart getCartByUser(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return cartRepository.save(newCart);
                });
    }

    @Override
    @Transactional
    public Cart addToCart(User user, Long courseId) {
        Cart cart = getCartByUser(user);

        // Check if course exists
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Khóa học không tồn tại"));

        // Check if already in cart
        boolean alreadyInCart = cart.getItems().stream()
                .anyMatch(item -> item.getCourse().getId().equals(courseId));
        if (alreadyInCart) {
            throw new IllegalStateException("Khóa học đã có trong giỏ hàng");
        }

        // Check if already bought
        if (orderDetailRepository.existsByOrderUserIdAndCourseId(user.getId(), courseId)) {
            throw new IllegalStateException("Bạn đã mua khóa học này rồi");
        }

        CartItem item = new CartItem(cart, course);
        cart.getItems().add(item);
        cart.updateTotalPrice();

        cartItemRepository.save(item);
        return cartRepository.save(cart);
    }

    @Override
    @Transactional
    public Cart removeFromCart(User user, Long cartItemId) {
        Cart cart = getCartByUser(user);
        
        Optional<CartItem> itemToRemove = cart.getItems().stream()
                .filter(item -> item.getId().equals(cartItemId))
                .findFirst();

        if (itemToRemove.isPresent()) {
            cart.getItems().remove(itemToRemove.get());
            cartItemRepository.delete(itemToRemove.get());
            cart.updateTotalPrice();
            return cartRepository.save(cart);
        }

        return cart;
    }

    @Override
    @Transactional
    public List<OrderResponse> checkout(User user) {
        Cart cart = getCartByUser(user);
        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Giỏ hàng trống");
        }

        Order order = new Order();
        order.setUser(user);
        order.setTotalAmount(cart.getTotalAmount());
        order.setStatus("COMPLETED");
        order.setOrderDate(LocalDateTime.now());
        
        Order savedOrder = orderRepository.save(order);
        List<OrderResponse> responses = new ArrayList<>();

        for (CartItem item : cart.getItems()) {
            // Check if user already bought it somehow before checking out
            if (!orderDetailRepository.existsByOrderUserIdAndCourseId(user.getId(), item.getCourse().getId())) {
                OrderDetail detail = new OrderDetail(savedOrder, item.getCourse(), item.getCourse().getPrice());
                OrderDetail savedDetail = orderDetailRepository.save(detail);

                OrderResponse response = new OrderResponse();
                response.setOrderId(savedOrder.getId());
                response.setCourse(savedDetail.getCourse());
                response.setPrice(savedDetail.getPrice());
                response.setOrderDate(savedOrder.getOrderDate());
                response.setStatus(savedOrder.getStatus());
                responses.add(response);
            }
        }

        // Clear cart
        cartItemRepository.deleteAll(cart.getItems());
        cart.getItems().clear();
        cart.updateTotalPrice();
        cartRepository.save(cart);

        return responses;
    }
}
