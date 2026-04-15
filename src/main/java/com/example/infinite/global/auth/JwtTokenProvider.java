package com.example.infinite.global.auth;

import io.jsonwebtoken.Claims;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final UserDetailsService userDetailsService;


    @Value("${jwt.secret}")
    private String secretKey;

    private SecretKey key;

    @Value("${jwt.expiration:1800000}")
    private long validityInMilliseconds;

    @PostConstruct
    protected void init() {
        // 보안상 문자열 키를 바이트 배열로 변환하여 HMAC-SHA 알고리즘 키로 로딩합니다.
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);

        // 키 길이 검증 (HS256을 위해 최소 256비트/32바이트 확보)
        if (keyBytes.length < 32) {
            log.error("JWT 시크릿 키가 너무 짧습니다. 최소 32바이트(256비트)가 필요합니다.");
            throw new IllegalArgumentException("JWT 시크릿 키의 길이가 부족합니다. 현재 길이: " + keyBytes.length);
        }

        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    // 1. 토큰 생성
    public String createToken(String userEmail, String role) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        //TODO : 프로젝트 윤곽이 나왔을때 issuer와 Audience 설정
        return Jwts.builder()
                .subject(userEmail)
                .claim("role", role)
                .claim("type", "ACCESS")
                .issuedAt(now)
                .expiration(validity)
                .signWith(key, Jwts.SIG.HS256) // 알고리즘 명시로 혼동 공격 방어
                .compact();
    }

    // 2. 토큰 파싱 및 검증 // TODO : 검증 역시 issuer와 Audience 추가 예정
    public Claims validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // 토큰 타입 검증 (ACCESS 타입이 아니면 예외를 던짐)
            String type = claims.get("type", String.class);
            if (!"ACCESS".equals(type)) {
                log.warn("잘못된 토큰 타입입니다. 기대값: ACCESS, 실제값: {}", type);
                throw new JwtException("Invalid token type");
            }

            return claims; // 검증 성공 시 Claims 반환
        }
        catch (io.jsonwebtoken.security.SignatureException e) {
            log.warn("잘못된 JWT 서명입니다: {}", e.getMessage());
            throw e;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰입니다: {}", e.getMessage());
            throw e;
        } catch (io.jsonwebtoken.UnsupportedJwtException e) {
            log.warn("지원되지 않는 JWT 토큰입니다: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.warn("JWT 토큰이 비어있거나 잘못되었습니다: {}", e.getMessage());
            throw e;
        } catch (JwtException e) {
            log.error("JWT 검증 중 예상치 못한 오류 발생: {}", e.getMessage());
            throw e;
        }
    }

    // 3. 인증 객체 생성


    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(claims.getSubject());

        // 토큰에 담긴 role을 꺼내서 시큐리티 권한 객체로 변환
        String role = claims.get("role", String.class);
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(role));

        // Principal에 이메일을 넣고, 권한 리스트를 부여
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


}
