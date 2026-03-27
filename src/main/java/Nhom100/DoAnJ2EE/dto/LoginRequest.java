package Nhom100.DoAnJ2EE.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}