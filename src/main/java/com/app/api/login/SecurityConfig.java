package com.app.api.login;

import com.app.api.login.jwt.JwtAuthenticationFilter;
import com.app.api.login.jwt.JwtSecurityService;
import com.app.api.login.jwt.JwtTokenProvider;
import com.app.api.login.session.SessionSecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE) // ✅ CORS 설정이 먼저 적용되도록 우선순위 지정
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;
    private final SessionSecurityService sessionSecurityService;
    private final JwtSecurityService jwtSecurityService; // ✅ JWT 인증 서비스 추가
    private final JwtTokenProvider jwtTokenProvider; // ✅ 추가
    private final PasswordEncoder passwordEncoder; // ✅ AppConfig에서 자동 주입

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource)) // ✅ CORS 활성화
                .csrf(csrf -> csrf.disable()) // ✅ CSRF 보호 비활성화
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/session/**").permitAll()
                        .requestMatchers("/jwt/**").permitAll()
                        .requestMatchers("/oauth2/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class) // ✅ JWT 필터 추가
                .formLogin(login -> login.disable()) // ✅ 기본 로그인 폼 비활성화
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // ✅ 세션을 어떻게 관리할지 결정하는 설정(세션 로그인일 시 ALWAYS, JWT 기반일 시 STATELESS  사용)
                );

        return http.build();
    }
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider); // ✅ JwtSecurityService 주입
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() { // ✅ PasswordEncoder 직접 주입 X
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(sessionSecurityService); // ✅ UserDetailsService 설정
        authProvider.setPasswordEncoder(passwordEncoder); // ✅ 자동 주입된 PasswordEncoder 사용
        return authProvider;
    }

}