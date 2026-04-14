package com.example.infinite.global.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class JwtTokenProvider {
    @Value("${JWT_SECRET_KEY}")
    private String secretKey;

    private SecretKey key;

    @Value("${jwt.expiration:1800000}")
    private long validityInMilliseconds;

    @PostConstruct
    protected void init() {
        // 보안상 문자열 키를 바이트 배열로 변환하여 HMAC-SHA 알고리즘 키로 로딩합니다.
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    // 1. 토큰 생성
    public String createToken(String userEmail, String role) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .subject(userEmail)
                .claim("role", role)
                .issuedAt(now)
                .expiration(validity)
                .signWith(key, Jwts.SIG.HS256) // 알고리즘 명시로 혼동 공격 방어
                .compact();
    }

    // 2. 토큰 파싱 및 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    // 3. 인증 객체 생성
    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token);

        // 토큰에 담긴 role을 꺼내서 시큐리티 권한 객체로 변환
        String role = claims.get("role", String.class);
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(role));

        // Principal에 이메일을 넣고, 권한 리스트를 부여합니다.
        return new UsernamePasswordAuthenticationToken(claims.getSubject(), "", authorities);
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
