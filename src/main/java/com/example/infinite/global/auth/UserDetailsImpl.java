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
@RequiredArgsConstructor
public class UserDetailsImpl implements UserDetails {

    private final User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
                new SimpleGrantedAuthority(user.getRole().getAuthority())
        );
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    /**
     * 계정 만료, 잠금, 활성화 여부 등은
     * 우선 true로 설정하고 나중에 User 엔티티의 status 필드와 연동 가능합니다.
     */
    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() {
        // 예: status가 "ACTIVE"인 경우에만 true를 반환하게 설계 가능
        return "ACTIVE".equals(user.getStatus());
    }
}
