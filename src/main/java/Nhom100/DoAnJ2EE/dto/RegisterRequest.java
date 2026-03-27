package Nhom100.DoAnJ2EE.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String password;
}