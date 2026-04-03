package Nhom100.DoAnJ2EE.controller;

import Nhom100.DoAnJ2EE.entity.Course;
import Nhom100.DoAnJ2EE.repository.CourseRepository;
import Nhom100.DoAnJ2EE.repository.CourseReviewRepository;
import Nhom100.DoAnJ2EE.repository.OrderDetailRepository;
import Nhom100.DoAnJ2EE.repository.UserRepository;
import Nhom100.DoAnJ2EE.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseReviewRepository courseReviewRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private UserRepository userRepository;

    // ✅ Lấy tất cả khóa học
    @GetMapping
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    // ✅ Lấy khóa học theo id
    @GetMapping("/{id}")
    public Course getCourseById(@PathVariable Long id) {
        return courseRepository.findById(id).orElse(null);
    }

    // ✅ Thêm khóa học
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public Course createCourse(@RequestBody Course course) {
        return courseRepository.save(course);
    }

    // ✅ Cập nhật khóa học
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public Course updateCourse(@PathVariable Long id, @RequestBody Course newCourse) {

        Course course = courseRepository.findById(id).orElse(null);

        if (course != null) {
            course.setName(newCourse.getName());
            course.setTitle(newCourse.getTitle());
            course.setDescription(newCourse.getDescription());
            course.setImage(newCourse.getImage());
            course.setPrice(newCourse.getPrice());

            return courseRepository.save(course);
        }

        return null;
    }

    // ✅ Xóa khóa học
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public String deleteCourse(@PathVariable Long id) {
        courseRepository.deleteById(id);
        return "Deleted successfully!";
    }

    // ✅ Gợi ý khóa học
    @GetMapping("/recomment")
    public Map<String, List<Course>> recommendCourses() {
        Long userId = getAuthenticatedUserId();
        Set<Long> purchasedCourseIds = new HashSet<>();
        if (userId != null) {
            orderDetailRepository.findByOrderUserId(userId)
                    .forEach(od -> purchasedCourseIds.add(od.getCourse().getId()));
        }

        List<Course> byCategory = new ArrayList<>();
        if (userId != null) {
            List<Long> categoryIds = orderDetailRepository.findPurchasedCategoryIds(userId);
            if (categoryIds != null && !categoryIds.isEmpty()) {
                for (Course course : courseRepository.findByCategoryIdIn(categoryIds)) {
                    if (!purchasedCourseIds.contains(course.getId())) {
                        byCategory.add(course);
                    }
                }
            }
        }

        List<Course> topRated = new ArrayList<>();
        Map<Long, Course> courseById = new HashMap<>();
        for (Course c : courseRepository.findAll()) {
            courseById.put(c.getId(), c);
        }
        for (CourseReviewRepository.CourseRatingSummary s : courseReviewRepository.findTopRatedCourses()) {
            Course c = courseById.get(s.getCourseId());
            if (c != null && !purchasedCourseIds.contains(c.getId())) {
                topRated.add(c);
            }
        }

        List<Course> trending = new ArrayList<>();
        for (OrderDetailRepository.CourseTrendingSummary s : orderDetailRepository.findTrendingCourses()) {
            Course c = courseById.get(s.getCourseId());
            if (c != null && !purchasedCourseIds.contains(c.getId())) {
                trending.add(c);
            }
        }

        Map<String, List<Course>> result = new LinkedHashMap<>();
        result.put("byCategory", limitList(byCategory, 6));
        result.put("topRated", limitList(topRated, 6));
        result.put("trending", limitList(trending, 6));
        return result;
    }

    private List<Course> limitList(List<Course> list, int max) {
        if (list == null) return List.of();
        return list.size() <= max ? list : list.subList(0, max);
    }

    private Long getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal().toString())) {
            String email = auth.getName();
            User user = userRepository.findByEmail(email).orElse(null);
            return user != null ? user.getId() : null;
        }
        return null;
    }
}
