package com.app.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security를 사용 중이라면 SecurityFilterChain에 .cors()를 추가하여 CORS 설정을 활성화해야함
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true); // ✅ 인증 정보(쿠키, 헤더 등) 포함 허용
        config.addAllowedHeader("*"); // ✅ 모든 헤더 허용
        config.addAllowedMethod("*"); // ✅ 모든 HTTP 메서드 허용
        config.setAllowedOriginPatterns(List.of("http://localhost:8085")); // ✅ 특정 Origin 허용

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
