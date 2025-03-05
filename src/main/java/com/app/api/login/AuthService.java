package com.app.api.login;

import com.app.api.login.jwt.JwtUtil;
import com.app.api.login.jwt.LoginRequest;
import com.app.api.login.jwt.LoginResponse;
import com.app.api.test.entity.UserEntity;
import com.app.api.test.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request){
        UserEntity user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if(!passwordEncoder.matches(request.getPassword(), user.getPassword())){
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user.getUsername() , user.getType().name());

        log.debug("✅ 생성된 JWT 토큰: {}", token);
        return new LoginResponse(token, user.getType());

    }

}
