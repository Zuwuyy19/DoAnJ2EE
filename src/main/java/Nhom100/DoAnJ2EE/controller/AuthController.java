package Nhom100.DoAnJ2EE.controller;

import Nhom100.DoAnJ2EE.dto.LoginRequest;
import Nhom100.DoAnJ2EE.dto.RegisterRequest;
import Nhom100.DoAnJ2EE.service.AuthService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

    // ===== LOGIN (form login cho web - trả về redirect) =====
    @PostMapping("/login")
    public String login(LoginRequest request) {

        boolean isSuccess = authService.loginWeb(request);

        if (isSuccess) {
            return "redirect:/";
        } else {
            return "redirect:/login?error=true";
        }
    }

    // ===== API LOGIN (trả về JWT token cho REST API) =====
    // POST /auth/api/login
    // Body: { "email": "...", "password": "..." }
    // Response: { "token": "..." }
    @PostMapping("/api/login")
    @ResponseBody
    public ResponseEntity<?> apiLogin(@RequestBody LoginRequest request) {
        String token = authService.login(request);

        if ("Login failed!".equals(token)) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Email hoặc mật khẩu không đúng"));
        }

        return ResponseEntity.ok(Map.of("token", token));
    }
}