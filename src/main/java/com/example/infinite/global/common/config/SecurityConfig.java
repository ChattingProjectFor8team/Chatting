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
                //    MemberRole enum name과 hasRole 값 정합성 유지 (MEMBER / ARTIST / SUPER_ADMIN)
                //    SUBSCRIBER는 Role이 아니라 구독 상태이므로 서비스 레이어에서 검증
                .authorizeHttpRequests(auth -> auth
                                // 1. 전체 공개
                                .requestMatchers("/api/auth/v1/**", "/h2-console/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/post/v1/fan-posts/**", "/api/post/v1/artist-posts/**", "/api/media/v1/**").permitAll()

                                // 2. SUPER_ADMIN 전용
                                .requestMatchers("/api/v1/admin/members/**").hasRole("SUPER_ADMIN")
                                .requestMatchers("/api/payment/v1/charge/settings/**").hasRole("SUPER_ADMIN")

                                // 3. ARTIST + SUPER_ADMIN
                                .requestMatchers("/api/post/v1/artist-posts").hasAnyRole("ARTIST", "SUPER_ADMIN")
                                .requestMatchers("/api/v1/admin/lives/**").hasAnyRole("ARTIST", "SUPER_ADMIN")
                                .requestMatchers("/api/v1/admin/raffles/**").hasAnyRole("ARTIST", "SUPER_ADMIN")
                                .requestMatchers("/api/v1/admin/artists/*/raffles").hasAnyRole("ARTIST", "SUPER_ADMIN")
                                .requestMatchers("/api/media/v1/media/import-youtube").hasAnyRole("ARTIST", "SUPER_ADMIN")
                                .requestMatchers(HttpMethod.POST, "/api/media/v1/**").hasAnyRole("ARTIST", "SUPER_ADMIN")
                                .requestMatchers(HttpMethod.DELETE, "/api/media/v1/**").hasAnyRole("ARTIST", "SUPER_ADMIN")

                                // 4. 인증된 사용자 (구독 검증은 서비스 레이어에서)
                                .requestMatchers("/api/post/v1/fan-letters/**").authenticated()
                                .requestMatchers("/api/payment/v1/subscription/**").authenticated()
                                .requestMatchers("/api/post/v1/fan-posts").authenticated()
                                .requestMatchers("/api/post/v1/comments/**", "/api/post/v1/*/likes/toggle").authenticated()
                                .requestMatchers("/api/myinfo/v1/**", "/api/payment/v1/jelly/**").authenticated()
                                .requestMatchers("/sub/user/{userId}/notifications").authenticated()

                                // 5. WebSocket 핸드쉐이크
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
