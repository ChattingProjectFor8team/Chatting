package com.example.infinite.global.auth;


import com.example.infinite.domain.member.member.entity.Member;
import com.example.infinite.domain.member.member.enums.MemberStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Spring Security 인증 객체 내부에서 멤버 정보를 담는 구현체입니다.
 * 리더님의 설계에 따라 DB 조회 기반 생성과 토큰 정보 기반 생성을 모두 지원합니다.
 */
@Getter
public class MemberDetailsImpl implements UserDetails {

    private final Long memberId;
    private final String email;
    private final String role;
    private final String status;

    // 1. DB 엔티티 기반 생성자
    public MemberDetailsImpl(Member member) {
        this.memberId = member.getId();
        this.email = member.getEmail();
        this.role = "ROLE_" + member.getRole().name();
        this.status = member.getStatus().name();
    }

    // 2. 토큰 기반 생성자 (동일하게 처리)
    private MemberDetailsImpl(String email, String role, Long memberId) {
        this.memberId = memberId;
        this.email = email;

        this.role = role.startsWith("ROLE_") ? role : "ROLE_" + role;

        this.status = MemberStatus.ACTIVE.name();
    }
    /**
     * 토큰에서 추출한 정보만으로 인증 객체를 생성하는 정적 팩토리 메서드입니다.
     * 이 메서드 덕분에 매 요청마다 DB Select 쿼리가 발생하는 부하를 원천 차단합니다.
     */
    public static MemberDetailsImpl fromToken(String email, String role, Long memberId) {
        return new MemberDetailsImpl(email, role, memberId);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        // 토큰 기반 인증이므로 비밀번호는 보관하지 않습니다.
        return null;
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    // 계정 상태 체크 로직 (리더님의 성실한 방어 설계)
    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() {
        // 멤버의 상태가 ACTIVE일 때만 접근을 허용합니다.
        return MemberStatus.ACTIVE.name().equals(this.status);
    }
}
