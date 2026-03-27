package Nhom100.DoAnJ2EE.service.impl;

import Nhom100.DoAnJ2EE.dto.LoginRequest;
import Nhom100.DoAnJ2EE.dto.RegisterRequest;
import Nhom100.DoAnJ2EE.entity.Role;
import Nhom100.DoAnJ2EE.entity.User;
import Nhom100.DoAnJ2EE.repository.RoleRepository;
import Nhom100.DoAnJ2EE.repository.UserRepository;
import Nhom100.DoAnJ2EE.config.JwtUtil;
import Nhom100.DoAnJ2EE.service.AuthService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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