package Nhom100.DoAnJ2EE.service;

import java.util.Arrays;

import java.util.List;

import org.springframework.stereotype.Service;

import Nhom100.DoAnJ2EE.model.Course;

@Service

public class CourseService {

    public List<Course> getAllCourses() {

        return Arrays.asList(

            new Course(1L, "Java Programming", "Java Programming", "Học lập trình Java từ cơ bản đến nâng cao", "https://via.placeholder.com/300x200?text=Java", 99.99),

            new Course(2L, "Spring Boot", "Spring Boot Masterclass", "Hướng dẫn toàn diện về Spring Boot", "https://via.placeholder.com/300x200?text=Spring+Boot", 149.99),

            new Course(3L, "J2EE", "Java Enterprise Edition", "Phát triển ứng dụng doanh nghiệp với J2EE", "https://via.placeholder.com/300x200?text=J2EE", 199.99)

        );

    }

}