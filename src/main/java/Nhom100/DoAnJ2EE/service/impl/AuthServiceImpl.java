package Nhom100.DoAnJ2EE.service.impl;

import Nhom100.DoAnJ2EE.dto.LoginRequest;
import Nhom100.DoAnJ2EE.dto.RegisterRequest;
import Nhom100.DoAnJ2EE.entity.Role;
import Nhom100.DoAnJ2EE.entity.User;
import Nhom100.DoAnJ2EE.repository.RoleRepository;
import Nhom100.DoAnJ2EE.repository.UserRepository;
import Nhom100.DoAnJ2EE.repository.CategoryRepository;
import Nhom100.DoAnJ2EE.config.JwtUtil;
import Nhom100.DoAnJ2EE.entity.Category;
import Nhom100.DoAnJ2EE.service.AuthService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import jakarta.annotation.PostConstruct;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {
        // Khởi tạo ROLE_USER và ROLE_ADMIN nếu chưa có
        Role userRole = roleRepository.findByName("ROLE_USER");
        if (userRole == null) {
            userRole = new Role();
            userRole.setName("ROLE_USER");
            roleRepository.save(userRole);
        }

        Role adminRole = roleRepository.findByName("ROLE_ADMIN");
        if (adminRole == null) {
            adminRole = new Role();
            adminRole.setName("ROLE_ADMIN");
            roleRepository.save(adminRole);
        }

        // Tạo sẵn một tài khoản Admin để test
        if (userRepository.findByEmail("admin@gmail.com") == null) {
            User admin = new User();
            admin.setEmail("admin@gmail.com");
            admin.setPassword(passwordEncoder.encode("123456"));
            admin.setRoles(new HashSet<>());
            admin.getRoles().add(adminRole);
            userRepository.save(admin);
        }

        // Khởi tạo Danh mục mẫu nếu chưa có
        if (categoryRepository.count() == 0) {
            String[] defaultCategories = {"Lập trình Java", "Web Front-End", "Digital Marketing", "Kỹ năng mềm"};
            for (String name : defaultCategories) {
                Category cat = new Category();
                cat.setName(name);
                categoryRepository.save(cat);
            }
        }
    }

    @Override
    public String register(RegisterRequest request) {

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Role role = roleRepository.findByName("ROLE_USER");

        user.setRoles(new HashSet<>());
        user.getRoles().add(role);

        userRepository.save(user);

        return "Register success!";
    }

    @Override
    public String login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail());

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return "Login failed!";
        }

        return JwtUtil.generateToken(user.getEmail());
    }
    @Override
    public boolean loginWeb(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail());

        if (user == null) return false;

        return passwordEncoder.matches(
            request.getPassword(),
            user.getPassword()
        );
    }    
}