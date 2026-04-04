package Nhom100.DoAnJ2EE.config;

import Nhom100.DoAnJ2EE.util.CurrencyUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Thymeleaf configuration — expose utility beans
 */
@Configuration
public class ThymeleafConfig {

    @Bean
    public CurrencyUtils currencyUtils() {
        return new CurrencyUtils();
    }
}
