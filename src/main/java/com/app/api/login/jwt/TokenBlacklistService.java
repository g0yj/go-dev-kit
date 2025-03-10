package com.app.api.login.jwt;

import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class TokenBlacklistService {
    private final Set<String> blacklist = new HashSet<>();

    /**
     * ✅ RefreshToken 블랙리스트에 추가
     */
    public void addToBlacklist(String token) {
        blacklist.add(token);
    }

    /**
     * ✅ RefreshToken이 블랙리스트에 있는지 확인
     */
    public boolean isBlacklisted(String token) {
        return blacklist.contains(token);
    }

}
