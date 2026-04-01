package Nhom100.DoAnJ2EE.config;

import Nhom100.DoAnJ2EE.entity.Role;
import Nhom100.DoAnJ2EE.entity.User;
import Nhom100.DoAnJ2EE.repository.RoleRepository;
import Nhom100.DoAnJ2EE.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {

        // ✅ Tạo ROLE_USER nếu chưa có
        Role roleUser = roleRepository.findByName("ROLE_USER");
        if (roleUser == null) {
            roleUser = new Role();
            roleUser.setName("ROLE_USER");
            roleRepository.save(roleUser);
        }

        // ✅ Tạo ROLE_ADMIN nếu chưa có
        Role roleAdmin = roleRepository.findByName("ROLE_ADMIN");
        if (roleAdmin == null) {
            roleAdmin = new Role();
            roleAdmin.setName("ROLE_ADMIN");
            roleRepository.save(roleAdmin);
        }

        // ✅ Tạo admin nếu chưa có
        User admin = userRepository.findByEmail("admin123@gmail.com");

        if (admin == null) {
            admin = new User();
            admin.setEmail("admin123@gmail.com");
            admin.setPassword(passwordEncoder.encode("123456"));

            Set<Role> roles = new HashSet<>();
            roles.add(roleAdmin); // 🔥 GÁN ADMIN

            admin.setRoles(roles);

            userRepository.save(admin);

            System.out.println("🔥 Admin created: admin123@gmail.com / 123456");
        }
    }
}