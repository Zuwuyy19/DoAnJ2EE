package Nhom100.DoAnJ2EE.service.impl;

import Nhom100.DoAnJ2EE.dto.CreateOrderRequest;
import Nhom100.DoAnJ2EE.dto.OrderResponse;
import Nhom100.DoAnJ2EE.entity.Course;
import Nhom100.DoAnJ2EE.entity.Order;
import Nhom100.DoAnJ2EE.entity.OrderDetail;
import Nhom100.DoAnJ2EE.entity.User;
import Nhom100.DoAnJ2EE.repository.CourseRepository;
import Nhom100.DoAnJ2EE.repository.OrderDetailRepository;
import Nhom100.DoAnJ2EE.repository.OrderRepository;
import Nhom100.DoAnJ2EE.repository.UserRepository;
import Nhom100.DoAnJ2EE.service.OrderService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
}

