package Nhom100.DoAnJ2EE.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import Nhom100.DoAnJ2EE.service.CustomUserDetailsService;

@Configuration
public class SecurityConfig {

    // ✅ Encode password
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ✅ Config security
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            // ❌ Tắt CSRF (dễ test)
            .csrf(csrf -> csrf.disable())

            // ✅ Phân quyền
            .authorizeHttpRequests(auth -> auth

                // 🔥 Public (không cần login)
                .requestMatchers(
                        "/",
                        "/login",
                        "/register",
                        "/auth/**",
                        "/css/**",
                        "/js/**",
                        "/images/**"
                ).permitAll()

                // 🔒 Các request khác cần login
                .anyRequest().authenticated()
            )

            // ✅ Form login
            .userDetailsService(userDetailsService)
            .formLogin(form -> form
                    .loginPage("/login")              // trang login custom
                    .loginProcessingUrl("/login")     // URL xử lý login
                    .defaultSuccessUrl("/", true)     // login thành công → home
                    .failureUrl("/login?error=true")  // login fail
                    .permitAll()
            )

            // ✅ Logout
            .logout(logout -> logout
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/login?logout=true")
                    .permitAll()
            );

        return http.build();
    }
    @Autowired
    private CustomUserDetailsService userDetailsService;
}