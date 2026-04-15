package com.example.infinite.global.auth;


import com.example.infinite.domain.user.entity.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * 시큐리티 인증 객체 내부에서 사용자 정보를 담는 구현체
 */
@Getter
public class UserDetailsImpl implements UserDetails {

    private final String email;
    private final String role;
    private final String status;

    // 1. 기존 DB 조회용 생성자 (로그인 시 사용)
    public UserDetailsImpl(User user) {
        this.email = user.getEmail();
        this.role = user.getRole().getAuthority();
        this.status = user.getStatus();
    }

    // 2. 토큰 정보 기반 생성자 (API 호출 시 DB 조회 생략용)
    private UserDetailsImpl(String email, String role) {
        this.email = email;
        this.role = role;
        this.status = "ACTIVE"; // 토큰이 유효하면 일단 활성 유저로 간주
    }

    public static UserDetailsImpl fromToken(String email, String role) {
        return new UserDetailsImpl(email, role);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        return null; // 인증 완료된 토큰 기반이므로 비밀번호는 필요 없음
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() {
        return "ACTIVE".equals(this.status);
    }
}
