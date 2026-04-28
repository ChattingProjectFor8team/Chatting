package com.example.infinite.global.auth;

import com.example.infinite.global.error.ErrorCode;
import com.example.infinite.global.exception.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {


    // ──────────────────────────────────────────────
    // 1. Claim 키 상수 — 생성과 소비가 반드시 같은 키를 참조
    // ──────────────────────────────────────────────
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_MEMBER_ID = "memberId";
    private static final String CLAIM_TYPE = "type";
    private static final String TOKEN_TYPE_ACCESS = "ACCESS";

    @Value("${jwt.secret}")
    private String secretKey;

    private SecretKey key;

    @Value("${jwt.expiration}")
    private long validityInMilliseconds;

    @PostConstruct
    protected void init() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException(
                    "JWT 시크릿 키 길이 부족: 최소 32바이트 필요, 현재 " + keyBytes.length + "바이트");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    // ──────────────────────────────────────────────
    // 2. 토큰 생성
    // ──────────────────────────────────────────────
    public String createToken(String userEmail, String role, Long memberId) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .subject(userEmail)
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_MEMBER_ID, memberId)
                .claim(CLAIM_TYPE, TOKEN_TYPE_ACCESS)
                .issuedAt(now)
                .expiration(validity)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    // ──────────────────────────────────────────────
    // 3. 토큰 검증 → Claims 반환
    //    실패 시 예외를 그대로 throw (Filter에서 catch)
    // ──────────────────────────────────────────────
    public Claims validateToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // 토큰 타입 검증
        String type = claims.get(CLAIM_TYPE, String.class);
        if (!TOKEN_TYPE_ACCESS.equals(type)) {
            throw new JwtException("Invalid token type: " + type);
        }

        return claims;
    }

    // ──────────────────────────────────────────────
    // 4. 인증 객체 생성 — 이미 검증된 Claims를 직접 받음
    //    ⚠️ 기존: getAuthentication(String token) → 내부에서 또 파싱
    //    수정: getAuthentication(Claims claims) → 이중 파싱 제거
    // ──────────────────────────────────────────────
    public Authentication getAuthentication(Claims claims) {
        String role = claims.get(CLAIM_ROLE, String.class);
        Long memberId = claims.get(CLAIM_MEMBER_ID, Long.class);

        if (role == null) {
            throw new BusinessException(ErrorCode.INVALID_AUTHENTICATION);
        }

        UserDetails userDetails = MemberDetailsImpl.fromToken(
                claims.getSubject(),
                role,
                memberId
        );

        return new UsernamePasswordAuthenticationToken(
                userDetails, "", userDetails.getAuthorities());
    }




}
