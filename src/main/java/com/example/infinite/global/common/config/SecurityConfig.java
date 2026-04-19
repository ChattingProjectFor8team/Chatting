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

                // 2. 요청 권한 설정 (화이트리스트 운영)
                // TODO : 너무 초안이라 반드시 수정해야함
                .authorizeHttpRequests(auth -> auth
                                // 1. 전체 공개
                                .requestMatchers("/api/auth/v1/**", "/h2-console/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/post/v1/fan-posts/**", "/api/post/v1/artists/*/fan-posts/**", "/api/post/v1/hashtags/**", "/api/post/v1/artist-posts/**", "/api/media/v1/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/member/v1/artists/{artistId}").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/member/v2/artists/{artistId}").permitAll()

                                // 2. 어드민 및 관리자 전용
                                // ADMIN 권한은 현재 MemberRole.SUPER_ADMIN만 가지므로
                                // `/api/{server}/admin/{version}/...` 경로는 SUPER_ADMIN 전용이다.
                                .requestMatchers("/api/member/admin/v1/**", "/api/payment/v1/charge/settings/**").hasRole("ADMIN")
                                .requestMatchers("/api/post/v1/artist-posts").hasAnyRole("ARTIST", "ADMIN")
                                .requestMatchers(HttpMethod.PATCH, "/api/post/v1/artists/*/fan-posts/*").hasAnyRole("USER", "SUBSCRIBER", "ARTIST", "ADMIN")
                                .requestMatchers(HttpMethod.DELETE, "/api/post/v1/artists/*/fan-posts/*").hasAnyRole("USER", "SUBSCRIBER", "ARTIST", "ADMIN")
                                .requestMatchers("/api/media/v1/media/import-youtube").hasAnyRole("ARTIST", "ADMIN")
                                .requestMatchers(HttpMethod.POST, "/api/media/v1/**").hasAnyRole("ARTIST", "ADMIN")
                                .requestMatchers(HttpMethod.DELETE, "/api/media/v1/**").hasAnyRole("ARTIST", "ADMIN")

                                // 3. 유료 서비스 (구독팬 전용)
                                .requestMatchers("/api/post/v1/fan-letters/**", "/api/payment/v1/subscription/**").hasAnyRole("SUBSCRIBER", "ARTIST", "ADMIN")

                                // 4. 일반 사용자 및 공통 인증
                                .requestMatchers(HttpMethod.POST, "/api/post/v1/artists/*/fan-posts").hasAnyRole("USER", "SUBSCRIBER", "ARTIST", "ADMIN")
                                .requestMatchers("/api/post/v1/fan-posts").hasAnyRole("USER", "SUBSCRIBER", "ARTIST", "ADMIN")
                                .requestMatchers(HttpMethod.POST, "/api/post/v1/artists/*/fan-posts/*/likes/toggle").authenticated()
                                .requestMatchers("/api/post/v1/comments/**", "/api/post/v1/*/likes/toggle").authenticated()
                                .requestMatchers("/api/member/v1/**", "/api/payment/v1/jelly/**").authenticated()
                                .requestMatchers("/sub/user/{userId}/notifications").authenticated()

                                // 5. 핸드쉐이크만 허용
                                .requestMatchers("/ws-stomp/**").permitAll()
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
