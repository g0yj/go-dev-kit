package com.app.api.login.session;

import com.app.api.login.session.dto.SessionRequest;
import com.app.api.login.session.dto.SessionResponse;
import com.app.api.test.entity.UserEntity;
import com.app.api.test.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionAuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SessionResponse login(HttpServletRequest request, SessionRequest sessionRequest) {
        UserEntity user = userRepository.findByUsername(sessionRequest.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(sessionRequest.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        request.getSession().setAttribute("user", user);
        return new SessionResponse(user.getUsername(), user.getType());
    }

    public void logout(HttpServletRequest request) {
        request.getSession().invalidate();
    }
}
