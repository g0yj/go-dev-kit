package com.app.api.login.jwt.go;

import com.app.api.test.entity.UserEntity;
import com.app.api.test.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;


@Service
@Slf4j
public class CustomJwtUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository; // UserRepository만 주입받기

    @Autowired
    public CustomJwtUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("🔍 사용자 '{}' 로딩 시도", username);

        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("❌ 사용자 '{}'를 찾을 수 없습니다", username);
                    return new UsernameNotFoundException("사용자를 찾을 수 없습니다");
                });

        log.info("✅ 사용자 '{}' 로딩 성공", username);
        log.info("🔑 사용자 '{}' 권한 생성, 유형: {}", user.getUsername(), user.getType());

        // ✅ type이 null이면 오류 발생
        if (user.getType() == null) {
            log.error("🚨 DB에서 가져온 사용자 '{}'의 type 값이 NULL 입니다!", user.getUsername());
            throw new IllegalStateException("사용자의 type 값이 설정되지 않았습니다.");
        }

        // ✅ UserDetails 생성 시 ROLE_ 접두사 추가
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getType().name()))
        );
    }

}