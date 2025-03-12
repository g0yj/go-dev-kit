package com.app.api.login.test;

import com.app.api.login.UserType;
import com.app.api.test.entity.UserEntity;
import com.app.api.test.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * ✅ Spring Boot 실행 시 자동으로 샘플 유저 삽입
 * ✅ 이미 존재하는 유저는 중복 삽입하지 않음
 * ✅ 비밀번호는 BCrypt로 암호화하여 저장
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TestDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("샘플 유저 데이터 삽입 시작...");

        createUserIfNotExists("ownerUser", "password123", UserType.B);
        createUserIfNotExists("adminUser", "password123", UserType.A);
        createUserIfNotExists("memberUser", "password123", UserType.C);

        log.info("샘플 유저 데이터 삽입 완료.");
    }

    /**
     * 유저가 존재하지 않으면 새로운 유저를 추가하는 메서드
     */
    private void createUserIfNotExists(String username, String rawPassword, UserType type) {
        if (userRepository.findByUsername(username).isEmpty()) {
            String encodedPassword = passwordEncoder.encode(rawPassword);
            UserEntity user = new UserEntity(username, encodedPassword, type , true);
            userRepository.save(user);
            log.info("샘플 유저 추가 - username: {}, type: {}", username, type);
        } else {
            log.info("이미 존재하는 유저 - username: {}", username);
        }
    }
}
