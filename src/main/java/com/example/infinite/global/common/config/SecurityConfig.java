package com.example.infinite.global.common.config;

import com.example.infinite.global.auth.JwtAccessDeniedHandler;
import com.example.infinite.global.auth.JwtAuthenticationEntryPoint;
import com.example.infinite.global.auth.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth
                        // ── 1. 전체 공개 ──
                        .requestMatchers(
                                "/api/auth/v1/**",
                                "/h2-console/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // 공개 GET: 팬포스트, 해시태그, 아티스트 포스트, 미디어, 아티스트 상세
                        .requestMatchers(HttpMethod.GET,
                                "/api/post/v1/fan-posts/**",
                                "/api/post/v1/artists/*/fan-posts/**",
                                "/api/post/v1/hashtags/**",
                                "/api/post/v1/artist-posts/**",
                                "/api/media/v1/**",
                                "/api/member/v1/artists/{artistId}",
                                "/api/member/v2/artists/{artistId}"
                        ).permitAll()

                        // 공개 GET: 래플/라이브 목록 및 상세 (비회원도 조회 가능)
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/artists/*/raffles",
                                "/api/v1/artists/*/raffles/*",
                                "/api/v1/artists/*/lives",
                                "/api/v1/artists/*/lives/*",
                                "/api/v1/artists/*/lives/*/chat/messages"
                        ).permitAll()

                        // WebSocket 핸드쉐이크 (STOMP 연결은 ChannelInterceptor에서 JWT 검증)
                        .requestMatchers("/ws-stomp/**").permitAll()

                        // ── 2. SUPER_ADMIN 전용 ──
                        .requestMatchers("/api/member/admin/v1/**").hasRole("SUPER_ADMIN")


                        // ── 3. ARTIST + SUPER_ADMIN (관리자) ──
                        .requestMatchers("/api/v1/admin/artists/*/raffles/**").hasAnyRole("ARTIST", "SUPER_ADMIN")
                        .requestMatchers("/api/v1/admin/artists/*/lives/**").hasAnyRole("ARTIST", "SUPER_ADMIN")
                        .requestMatchers("/api/post/v1/artist-posts").hasAnyRole("ARTIST", "SUPER_ADMIN")
                        .requestMatchers("/api/media/v1/media/import-youtube").hasAnyRole("ARTIST", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/media/v1/**").hasAnyRole("ARTIST", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/media/v1/**").hasAnyRole("ARTIST", "SUPER_ADMIN")

                        // ── 4. 인증된 사용자 (로그인 필수) ──
                        // 래플 응모 및 내 응모 내역
                        .requestMatchers("/api/v1/artists/*/raffles/*/entries/**").authenticated()
                        .requestMatchers("/api/v1/users/me/raffle-entries").authenticated()

                        // 팬포스트 CRUD (작성/수정/삭제)
                        .requestMatchers(HttpMethod.POST, "/api/post/v1/artists/*/fan-posts").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/post/v1/artists/*/fan-posts/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/post/v1/artists/*/fan-posts/*").authenticated()
                        .requestMatchers("/api/post/v1/fan-posts").authenticated()

                        // 좋아요, 댓글
                        .requestMatchers(HttpMethod.POST, "/api/post/v1/artists/*/fan-posts/*/likes/toggle").authenticated()
                        .requestMatchers("/api/post/v1/comments/**", "/api/post/v1/*/likes/toggle").authenticated()

                        // 팬레터 (구독 검증은 서비스 레이어에서 수행)
                        .requestMatchers("/api/post/v1/fan-letters/**").authenticated()

                        // 회원, 결제, 구독, 알림
                        .requestMatchers("/api/member/v1/**").authenticated()
                        .requestMatchers("/api/payment/v1/**").authenticated()
                        .requestMatchers("/sub/user/{userId}/notifications").authenticated()
                )

                // 3. 예외 핸들링 (EntryPoint, DeniedHandler 운영)
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint) // 401 Unauthorized
                        .accessDeniedHandler(jwtAccessDeniedHandler)           // 403 Forbidden
                )

                // 4. JWT 필터 추가 (UsernamePasswordAuthenticationFilter 앞단에 배치)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
