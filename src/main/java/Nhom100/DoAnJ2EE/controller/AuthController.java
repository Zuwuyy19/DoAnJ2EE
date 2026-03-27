package Nhom100.DoAnJ2EE.controller;

import Nhom100.DoAnJ2EE.dto.LoginRequest;
import Nhom100.DoAnJ2EE.dto.RegisterRequest;
import Nhom100.DoAnJ2EE.service.AuthService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    // ===== REGISTER =====
    @PostMapping("/register")
    public String register(RegisterRequest request) {
        authService.register(request);
        return "redirect:/login"; // đăng ký xong về login
    }

    // ===== LOGIN =====
    @PostMapping("/login")
    public String login(LoginRequest request) {

        boolean isSuccess = authService.loginWeb(request);

        if (isSuccess) {
            return "redirect:/";
        } else {
            return "redirect:/login?error=true";
        }
    }
}