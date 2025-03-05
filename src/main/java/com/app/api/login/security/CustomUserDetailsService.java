package com.app.api.login.security;

import com.app.api.test.entity.UserEntity;
import com.app.api.test.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 사용자의 정보를 DB에서 불러와 인증
 * DB에서 사용자 정보를 가져오기 위해 UserRepository를 사용.
 * 사용자의 type을 Spring Security의 GrantedAuthority(권한)로 변환하여 반환.
 *
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity userEntity = userRepository.findByUsername(username)
                .orElseThrow(()-> new UsernameNotFoundException("User not found: " + username));

        return new User(
                userEntity.getUsername(),
                userEntity.getPassword(),
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" +userEntity.getType()))
        );
    }
}
