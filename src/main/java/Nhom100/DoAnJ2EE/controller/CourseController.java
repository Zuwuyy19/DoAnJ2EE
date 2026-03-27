package Nhom100.DoAnJ2EE.controller;

import Nhom100.DoAnJ2EE.entity.Course;
import Nhom100.DoAnJ2EE.repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/courses")
public class CourseController {

    @Autowired
    private CourseRepository courseRepository;

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
    @PostMapping
    public Course createCourse(@RequestBody Course course) {
        return courseRepository.save(course);
    }

    // ✅ Cập nhật khóa học
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
    @DeleteMapping("/{id}")
    public String deleteCourse(@PathVariable Long id) {
        courseRepository.deleteById(id);
        return "Deleted successfully!";
    }
}