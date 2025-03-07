package com.app.api.login;

import com.app.api.login.jwt.dto.JwtTokenRequest;
import com.app.api.login.jwt.dto.JwtTokenResponse;
import com.app.api.login.oauth2.OAuth2Service;
import com.app.api.login.oauth2.dto.OAuth2UserInfo;
import com.app.api.login.oauth2.go.OAuth2Service2;
import com.app.api.login.session.dto.SessionRequest;
import com.app.api.login.session.dto.SessionResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


@Controller
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final OAuth2Service oAuth2Service;

    private final OAuth2Service2 oAuth2Service2;
    public AuthController(AuthService authService, OAuth2Service oAuth2Service, OAuth2Service2 oAuth2Service2) {
        this.authService = authService;
        this.oAuth2Service = oAuth2Service;
        this.oAuth2Service2 = oAuth2Service2;
    }

    @GetMapping("/login")
    public String main(){
        log.debug("프로젝트 스타트");
        return "index";

    }
    @PostMapping("/session/login")
    @ResponseBody
    public ResponseEntity<SessionResponse> sessionLogin(@RequestBody SessionRequest request, HttpServletRequest httpRequest) {
        log.debug("Session 로그인 요청 - username: {}, type: {}", request.getUsername(), request.getType());

        try {
            SessionResponse response = authService.sessionLogin(httpRequest, request);
            log.info("Session 로그인 성공 - username: {}", request.getUsername());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Session 로그인 실패 - username: {}, 이유: {}", request.getUsername(), e.getMessage());
            return ResponseEntity.status(401).body(null);
        }
    }
    // ✅ 세션 로그아웃
    @ResponseBody
    @PostMapping("/session/logout")
    public ResponseEntity<String> sessionLogout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session != null) {
            String username = (String) session.getAttribute("username");
            log.info("Session 로그아웃 요청 - username: {}", username);

            authService.sessionLogout(request); // ✅ AuthService에서 DB에서 세션 삭제까지 처리
            return ResponseEntity.ok("Session 로그아웃 성공!");
        } else {
            log.warn("로그아웃 요청했지만 세션이 없음");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("이미 로그아웃된 상태입니다.");
        }
    }

    @PostMapping("/session/security/login")
    @ResponseBody
    public ResponseEntity<String> sessionSecurityLogin( HttpServletRequest httpRequest, @RequestBody SessionRequest request) {
        log.debug(" 로그인 요청 - username: {}", request.getUsername());

        boolean isAuthenticated = authService.sessionSecurityLogin(httpRequest, request);

        if (isAuthenticated) {
            return ResponseEntity.ok("로그인 성공!");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인 실패: 잘못된 인증 정보");
        }

    }

    @GetMapping("/session/security/status")
    @ResponseBody
    public ResponseEntity<String> getSessionStatus(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("username") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("❌ 로그인된 세션이 없습니다.");
        }

        String username = (String) session.getAttribute("username");
        return ResponseEntity.ok("✅ 로그인된 사용자: " + username);
    }


    @PostMapping("/session/security/logout")
    @ResponseBody
    public ResponseEntity<String> sessionSecurityLogout(HttpServletRequest request) {
        authService.sessionSecurityLogout(request);
        return ResponseEntity.ok("로그아웃 성공!");
    }

    @PostMapping("/jwt/login")
    @ResponseBody
    public ResponseEntity<JwtTokenResponse> jwtLogin(@RequestBody JwtTokenRequest jwtRequest) {
        return ResponseEntity.ok(authService.jwtLogin(jwtRequest));
    }

    @PostMapping("/jwt/logout")
    @ResponseBody
    public ResponseEntity<String> jwtLogout(@RequestHeader("Authorization") String authHeader) {
        log.debug("🚪 JWT 로그아웃 요청 - Authorization Header: {}", authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.error("❌ 잘못된 Authorization 헤더");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("잘못된 Authorization 헤더");
        }

        // ✅ "Bearer " 이후의 값이 Refresh Token이므로 추출
        String refreshToken = authHeader.substring(7).trim();
        log.debug("🛠 추출된 Refresh Token: {}", refreshToken);

        authService.jwtLogout(refreshToken);

        return ResponseEntity.ok("로그아웃 성공");
    }

    /**
     * ✅ 카카오 로그인 페이지로 이동
     */
    @GetMapping("/oauth2/kakao/login")
    public ResponseEntity<Void> redirectToKakaoLogin() {
        String kakaoLoginUrl = oAuth2Service.getKakaoLoginUrl();
        log.info("🔗 카카오 로그인 페이지로 리다이렉트: {}", kakaoLoginUrl);

        return ResponseEntity.status(HttpStatus.FOUND) // 302 Redirect
                .header(HttpHeaders.LOCATION, kakaoLoginUrl)
                .build();
    }

    /**
     * ✅ 카카오 로그인 후, 인가 코드를 받아 처리 (oauth2는 보통 세션 기반 로그인임)
     * ✅ 시큐리티를 사용하지 않고 로그인 하는 경우 로그인 페이지를 가져오는 건 kakaoCallback() 임
     *    -> Spring Security가 활성화된 경우에는 자동으로 처리되지만, 현재 Security 없이 직접 처리해야 함.
     *    -> 세션 기반 로그인을 위한 코드는 아래 주석 처리. 현재는 시큐리티 적용 로그인
     */
    @GetMapping("/oauth2/kakao/callback")
    public ResponseEntity<Map<String, String>> kakaoCallback(@RequestParam("code") String authorizationCode , HttpServletResponse response) {
        log.info("🔐 카카오 로그인 후 리디렉션됨 - 인증 코드: {}", authorizationCode);

        try {
            // 1. 카카오에서 액세스 토큰 요청
            String accessToken = oAuth2Service.getKakaoAccessToken(authorizationCode);
            log.info("🔑 카카오 액세스 토큰 발급 완료: {}", accessToken);

            // 2. 액세스 토큰으로 사용자 정보 요청
            OAuth2UserInfo userInfo = oAuth2Service.getKakaoUserInfo(accessToken);
            log.info("🔍 카카오 사용자 정보 조회 완료 - 이메일: {}, 닉네임: {}", userInfo.getEmail(), userInfo.getNickname());

            // 3. JWT 토큰 생성
            Map<String, String> tokens = oAuth2Service.generateTokens(userInfo);
            log.info("🔑 JWT 토큰 생성 완료 - AccessToken: {}", tokens.get("accessToken"));

            // 4. 응답 헤더에 JWT 추가
            response.setHeader("Authorization", "Bearer " + tokens.get("accessToken"));
            log.info("🔐 응답 헤더에 JWT 추가 완료");

            // 5. JWT 토큰을 클라이언트에 반환
            return ResponseEntity.ok(tokens);

        } catch (Exception e) {
            log.error("❌ 카카오 로그인 처리 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", "카카오 로그인 처리 중 오류 발생"));
        }
    }


    // ✅ OAuth2 로그아웃
    @PostMapping("/oauth2/kakao/logout")
    @ResponseBody
    public ResponseEntity<String> logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false); // 기존 세션만 가져오고 새로 생성 X

        if (session != null) {
            log.info("🆔 현재 세션 ID (무효화 전): {}", session.getId());
            session.invalidate(); // ✅ 세션 무효화
            log.info("✅ 세션 무효화 완료");

            // 🚀 세션 재생성을 방지하기 위해 쿠키 삭제
            Cookie cookie = new Cookie("JSESSIONID", null);
            cookie.setMaxAge(0);
            cookie.setPath("/");
            response.addCookie(cookie);
            log.info("🍪 JSESSIONID 쿠키 삭제 완료");
        } else {
            log.warn("⚠️ 세션이 존재하지 않음 (이미 로그아웃됨)");
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, "/login") // 로그인 페이지로 리다이렉트
                .build();
    }

    @GetMapping("/check-session")
    @ResponseBody
    public ResponseEntity<String> checkSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            log.info("✅ 세션이 없음 (정상 로그아웃 상태)");
            return ResponseEntity.ok("세션 없음 (정상 로그아웃)");
        } else {
            log.warn("⚠️ 세션이 아직 살아있음! ID: {}", session.getId());
            return ResponseEntity.ok("세션이 아직 살아있음! ID: " + session.getId());
        }
    }

    @GetMapping("/admin/main")
    public String adminMain(HttpServletRequest request, Model model) {

        // 세션에서 직접 loginType 확인
        HttpSession session = request.getSession(false);
        log.debug("session: {} ", session);

        if (session != null) {
            log.debug("세션에서 로그인 타입: {}", session.getAttribute("loginType"));
            log.debug("세션에서 사용자 이메일: {}", session.getAttribute("username"));
        }

        // 세션에서 loginType을 가져와서 모델에 추가
        if (session != null && session.getAttribute("loginType") != null) {
            model.addAttribute("loginType", session.getAttribute("loginType"));
        } else {
            log.debug("loginType : {} ", session.getAttribute("loginType"));
        }
        return "admin/main";
    }

    /**
     * ✅ OAuth2 인증 후 콜백 처리
     */
    @GetMapping("/{provider}/callback")
    @ResponseBody
    public ResponseEntity<Map<String, String>> oauth2Callback(
            @PathVariable String provider,
            @RequestParam(required = false) String code) {

        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("OAuth2 인증 코드가 없습니다.");
        }

        // 1️⃣ OAuth2 Access Token 요청
        String accessToken = oAuth2Service2.getAccessToken(code, provider);

        // 2️⃣ 사용자 정보 요청
        OAuth2UserInfo userInfo = oAuth2Service2.getUserInfo(accessToken, provider);

        // 3️⃣ JWT 토큰 생성
        Map<String, String> jwtTokens = oAuth2Service2.generateTokens(userInfo);

        // ✅ JSON 응답 형식 지정
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(jwtTokens);
    }
}




/**
 *         log.info("📥 카카오 OAuth2 로그인 성공 - 인가 코드: {}", code);
 *
 *         // 1️⃣ Access Token 요청
 *         String accessToken = oAuth2Service.getKakaoAccessToken(code);
 *         log.info("🔑 발급된 카카오 Access Token: {}", accessToken);
 *
 *         // 2️⃣ 사용자 정보 요청
 *         OAuth2UserInfo userInfo = oAuth2Service.getKakaoUserInfo(accessToken);
 *         log.info("✅ 카카오 사용자 정보: {}", userInfo);
 *
 *         // 3. 세션에 로그인 타입 저장
 *         HttpSession session = request.getSession(true);
 *         session.setAttribute("loginType", LoginType.KAKAO);
 *         session.setAttribute("username", userInfo.getEmail());
 *         log.info("✅ 로그인 타입 저장 완료 - loginType: KAKAO");
 *         log.info("✅ id : {}" , userInfo.getEmail());
 *
 *         // 세션 값 로깅
 *         log.debug("세션 저장 후 로그인 타입: {}", session.getAttribute("loginType"));
 *         log.debug("세션 저장 후 사용자 이메일: {}", session.getAttribute("username"));
 *
 *         return "redirect:/admin/main";
 */