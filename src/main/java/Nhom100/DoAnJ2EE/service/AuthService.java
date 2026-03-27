package Nhom100.DoAnJ2EE.service;

import Nhom100.DoAnJ2EE.dto.LoginRequest;
import Nhom100.DoAnJ2EE.dto.RegisterRequest;

public interface AuthService {
    String register(RegisterRequest request);
    String login(LoginRequest request);
    boolean loginWeb(LoginRequest request);
}
