package com.app.api.login.test;

import com.app.api.login.UserType;
import com.app.api.test.entity.UserEntity;
import com.app.api.test.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * ✅ 회원가입 기능을 제공 (새로운 유저 등록 가능)
 * ✅ 회원가입 시 UserEntity를 생성하여 DB에 저장
 * ✅ 비밀번호는 BCrypt로 암호화하여 저장
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입 처리 (DTO → Entity 변환 후 저장)
     */
    public void registerUser(String username, String password, UserType type, boolean isApproved) {
        log.info("회원가입 요청 - username: {}, type: {}", username, type);

        // 중복된 username 검사
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 사용자 이름입니다: " + username);
        }

        // 비밀번호 암호화 후 저장
        String encodedPassword = passwordEncoder.encode(password);
        UserEntity user = new UserEntity(username, encodedPassword, type , true);
        userRepository.save(user);

        log.info("회원가입 완료 - username: {}", username);
    }
}
