package Nhom100.DoAnJ2EE.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình Swagger/OpenAPI cho dự án
 * Swagger UI cho phép test API trực tiếp trên trình duyệt mà không cần Postman
 */
@Configuration
public class SwaggerConfig {

    // Bean OpenAPI: định nghĩa thông tin và cấu hình cho API documentation
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                // Thông tin API: tiêu đề, mô tả, phiên bản
                .info(new Info()
                        .title("API Đặt hàng khóa học")      // Tiêu đề API
                        .description("API cho chức năng mua khóa học")  // Mô tả API
                        .version("v1.0"));                          // Phiên bản
    }
}
