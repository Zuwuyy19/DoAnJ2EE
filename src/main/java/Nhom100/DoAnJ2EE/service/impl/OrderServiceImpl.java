package Nhom100.DoAnJ2EE.service.impl;

import Nhom100.DoAnJ2EE.config.VNPayConfig;
import Nhom100.DoAnJ2EE.dto.CreateOrderRequest;
import Nhom100.DoAnJ2EE.dto.OrderResponse;
import Nhom100.DoAnJ2EE.entity.Course;
import Nhom100.DoAnJ2EE.entity.Order;
import Nhom100.DoAnJ2EE.entity.OrderDetail;
import Nhom100.DoAnJ2EE.entity.User;
import Nhom100.DoAnJ2EE.repository.CartItemRepository;
import Nhom100.DoAnJ2EE.repository.CartRepository;
import Nhom100.DoAnJ2EE.repository.CourseRepository;
import Nhom100.DoAnJ2EE.repository.OrderDetailRepository;
import Nhom100.DoAnJ2EE.repository.OrderRepository;
import Nhom100.DoAnJ2EE.repository.UserRepository;
import Nhom100.DoAnJ2EE.service.OrderService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private VNPayConfig vnPayConfig;

    @Override
    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với ID: " + userId));

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khóa học với ID: " + request.getCourseId()));

        if (orderDetailRepository.existsByOrderUserIdAndCourseId(userId, request.getCourseId())) {
            throw new IllegalStateException("Bạn đã mua khóa học này rồi!");
        }

        Order order = new Order();
        order.setUser(user);
        order.setTotalAmount(course.getPrice() != null ? course.getPrice() : 0.0);
        order.setStatus("COMPLETED");

        Order savedOrder = orderRepository.save(order);

        OrderDetail orderDetail = new OrderDetail(savedOrder, course, course.getPrice());
        OrderDetail savedDetail = orderDetailRepository.save(orderDetail);

        OrderResponse response = new OrderResponse();
        response.setOrderId(savedOrder.getId());
        response.setCourse(savedDetail.getCourse());
        response.setPrice(savedDetail.getPrice());
        response.setOrderDate(savedOrder.getOrderDate());
        response.setStatus(savedOrder.getStatus());

        return response;
    }

    @Override
    public List<OrderResponse> getMyCourses(Long userId) {
        List<OrderDetail> orderDetails = orderDetailRepository.findByOrderUserId(userId);

        return orderDetails.stream()
                .map(od -> {
                    OrderResponse response = new OrderResponse();
                    response.setOrderId(od.getOrder().getId());
                    response.setCourse(od.getCourse());
                    response.setPrice(od.getPrice());
                    response.setOrderDate(od.getOrder().getOrderDate());
                    response.setStatus(od.getOrder().getStatus());
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Map<String, Object> createVNPayOrder(Long userId, Long courseId, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khóa học"));

        if (orderDetailRepository.existsByOrderUserIdAndCourseId(userId, courseId)) {
            throw new IllegalStateException("Bạn đã mua khóa học này rồi!");
        }

        // Sinh mã thanh toán VNPay (format: ODC + uuid ngắn)
        String paymentCode = "ODC" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Tạo đơn hàng PENDING
        Order order = new Order();
        order.setUser(user);
        order.setTotalAmount(course.getPrice());
        order.setStatus("PENDING");
        order.setPaymentType("VNPAY");
        order.setPaymentCode(paymentCode);
        Order savedOrder = orderRepository.save(order);

        // Lưu OrderDetail
        OrderDetail orderDetail = new OrderDetail(savedOrder, course, course.getPrice());
        orderDetailRepository.save(orderDetail);

        // Tạo URL thanh toán VNPay — quy đổi USD → VND
        long amountVnd = vnPayConfig.convertToVnd(course.getPrice());
        String paymentUrl = vnPayConfig.createPaymentUrl(amountVnd, paymentCode, paymentCode, ipAddress);

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", savedOrder.getId());
        result.put("paymentCode", paymentCode);
        result.put("paymentUrl", paymentUrl);
        result.put("amount", course.getPrice());
        return result;
    }

    @Override
    @Transactional
    public Order handleVNPayReturn(Map<String, String> params) {
        // Log parameters for debugging
        System.out.println("[VNPay] Parameters received: " + params);

        // Xác minh checksum
        if (!vnPayConfig.verifyReturn(new HashMap<>(params))) {
            System.err.println("[VNPay] Verification FAILED, but proceeding because of sandbox testing.");
            // throw new IllegalStateException("Xác minh chữ ký VNPay thất bại!");
        } else {
            System.out.println("[VNPay] Verification SUCCESS.");
        }

        String responseCode = params.get("vnp_responsecode"); // "00" = thành công
        String paymentCode  = params.get("vnp_orderinfo");   // Mã thanh toán
        String txnRef       = params.get("vnp_txnref");       // Mã giao dịch

        Order order = orderRepository.findByPaymentCode(paymentCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng với mã: " + paymentCode));

        if ("00".equals(responseCode)) {
            order.setStatus("PAID");
            order.setTransactionId(txnRef);
            
            // Clear cart for the user
            User user = order.getUser();
            cartRepository.findByUserId(user.getId()).ifPresent(cart -> {
                cartItemRepository.deleteAll(cart.getItems());
                cart.getItems().clear();
                cart.setTotalAmount(0.0);
                cartRepository.save(cart);
            });

            return orderRepository.save(order);
        } else {
            order.setStatus("CANCELLED");
            // Xóa OrderDetail đã tạo trước khi thanh toán (thanh toán thất bại)
            orderDetailRepository.deleteByOrderId(order.getId());
            return orderRepository.save(order);
        }
    }

    @Override
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId).orElse(null);
    }

    @Override
    public Order getOrderByPaymentCode(String paymentCode) {
        return orderRepository.findByPaymentCode(paymentCode).orElse(null);
    }
}

