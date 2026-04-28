package com.example.infinite.global.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
// Swagger/OpenAPI 메타 정보와 JWT 인증 방식을 문서 전역에 선언한다.
@OpenAPIDefinition(
        info = @Info(
                title = "Infinite Weverse Clone API",
                description = "홈 피드와 아티스트 커뮤니티 기능 중심의 API 문서",
                version = "v1.0.0",
                contact = @Contact(name = "Infinite Team")
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        // Swagger UI에 노출될 서비스 기본 정보를 구성한다.
        return new OpenAPI()
                .info(new io.swagger.v3.oas.models.info.Info()
                        .title("Infinite Weverse Clone API")
                        .description("Weverse 클론 프로젝트의 홈/아티스트 커뮤니티 API 문서")
                        .version("v1.0.0")
                        .license(new License().name("Internal Use Only")));
    }
}
