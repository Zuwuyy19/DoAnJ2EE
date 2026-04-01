package Nhom100.DoAnJ2EE.service.impl;

import Nhom100.DoAnJ2EE.dto.CreateOrderRequest;
import Nhom100.DoAnJ2EE.dto.OrderResponse;
import Nhom100.DoAnJ2EE.entity.Course;
import Nhom100.DoAnJ2EE.entity.Order;
import Nhom100.DoAnJ2EE.entity.User;
import Nhom100.DoAnJ2EE.repository.CourseRepository;
import Nhom100.DoAnJ2EE.repository.OrderRepository;
import Nhom100.DoAnJ2EE.repository.UserRepository;
import Nhom100.DoAnJ2EE.service.OrderService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation của OrderService - triển khai các nghiệp vụ liên quan đến đơn hàng
 * @Service: Đánh dấu đây là một Spring Service, tự động được quản lý bởi Spring Container
 */
@Service   // Đánh dấu đây là một Spring Service bean
public class OrderServiceImpl implements OrderService {

    // Inject OrderRepository - dùng để thao tác CRUD với bảng orders trong DB
    @Autowired
    private OrderRepository orderRepository;

    // Inject CourseRepository - dùng để tra cứu thông tin khóa học
    @Autowired
    private CourseRepository courseRepository;

    // Inject UserRepository - dùng để tra cứu thông tin người dùng
    @Autowired
    private UserRepository userRepository;

    /**
     * Tạo đơn hàng mới (mua khóa học)
     * Quy trình:
     *  1. Tìm người dùng theo userId
     *  2. Tìm khóa học theo courseId trong request
     *  3. Kiểm tra khóa học đã tồn tại chưa
     *  4. Kiểm tra người dùng đã mua khóa học này chưa (tránh mua trùng)
     *  5. Tạo và lưu đơn hàng vào DB
     * @Transactional: Đảm bảo toàn bộ quy trình được thực hiện trong 1 transaction
     *                Nếu có lỗi xảy ra, tất cả thay đổi sẽ được rollback
     */
    @Override
    @Transactional   // Đảm bảo tính toàn vẹn dữ liệu - nếu lỗi thì rollback
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {

        // Bước 1: Tìm người dùng trong DB theo userId
        // Nếu không tìm thấy → ném ngoại lệ IllegalArgumentException
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với ID: " + userId));

        // Bước 2: Tìm khóa học trong DB theo courseId trong request
        // Nếu không tìm thấy → ném ngoại lệ IllegalArgumentException
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khóa học với ID: " + request.getCourseId()));

        // Bước 3: Kiểm tra người dùng đã mua khóa học này chưa
        // existsByUserIdAndCourseId sẽ trả về true nếu đã có đơn hàng cho user-course này
        // Nếu đã mua rồi → ném ngoại lệ IllegalStateException (không cho phép mua trùng)
        if (orderRepository.existsByUserIdAndCourseId(userId, request.getCourseId())) {
            throw new IllegalStateException("Bạn đã mua khóa học này rồi!");
        }

        // Bước 4: Tạo đối tượng Order mới với thông tin đã có
        Order order = new Order();
        order.setUser(user);                       // Gán người dùng vào đơn hàng
        order.setCourse(course);                   // Gán khóa học vào đơn hàng
        order.setPrice(course.getPrice());         // Lưu giá tại thời điểm mua (lấy từ khóa học)
        order.setStatus("COMPLETED");              // Đặt trạng thái là HOÀN THÀNH (thanh toán thành công)

        // Bước 5: Lưu đơn hàng vào DB thông qua OrderRepository
        // save() sẽ INSERT bản ghi mới vào bảng orders
        Order savedOrder = orderRepository.save(order);

        // Bước 6: Chuyển đổi entity Order → DTO OrderResponse để trả về cho client
        // Không trả về entity gốc để tránh lộ thông tin nhạy cảm
        OrderResponse response = new OrderResponse();
        response.setOrderId(savedOrder.getId());              // Gán ID đơn hàng vừa tạo
        response.setCourse(savedOrder.getCourse());           // Gán thông tin khóa học
        response.setPrice(savedOrder.getPrice());            // Gán giá tại thời điểm mua
        response.setOrderDate(savedOrder.getOrderDate());    // Gán ngày giờ đặt mua
        response.setStatus(savedOrder.getStatus());          // Gán trạng thái đơn hàng

        // Trả về OrderResponse cho controller
        return response;
    }

    /**
     * Lấy danh sách khóa học đã mua của một người dùng
     * Trả về danh sách đơn hàng (đã hoàn thành) kèm thông tin khóa học
     * Sử dụng JOIN FETCH để lấy course trong 1 câu SQL (tránh N+1 query)
     * @param userId ID của người dùng cần tra cứu danh sách khóa học đã mua
     * @return Danh sách OrderResponse chứa thông tin các khóa học đã mua
     */
    @Override
    public List<OrderResponse> getMyCourses(Long userId) {

        // Gọi repository để lấy danh sách đơn hàng của user, kèm thông tin khóa học
        // findByUserIdWithCourse sử dụng JPQL với JOIN FETCH
        // Điều này đảm bảo chỉ 1 câu SQL được thực thi thay vì N+1 câu
        List<Order> orders = orderRepository.findByUserIdWithCourse(userId);

        // Chuyển đổi danh sách entity Order → danh sách DTO OrderResponse
        // Sử dụng Stream API của Java 8+ để map từng Order thành OrderResponse
        return orders.stream()
                // map: chuyển đổi mỗi Order thành OrderResponse
                .map(order -> {
                    OrderResponse response = new OrderResponse();
                    response.setOrderId(order.getId());                  // Gán ID đơn hàng
                    response.setCourse(order.getCourse());               // Gán thông tin khóa học
                    response.setPrice(order.getPrice());                 // Gán giá tại thời điểm mua
                    response.setOrderDate(order.getOrderDate());          // Gán ngày giờ đặt mua
                    response.setStatus(order.getStatus());                // Gán trạng thái đơn hàng
                    return response;                                     // Trả về OrderResponse đã được map
                })
                // collect: gom kết quả thành List<OrderResponse>
                .collect(Collectors.toList());
    }
}
