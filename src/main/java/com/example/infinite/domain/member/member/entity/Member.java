package com.example.infinite.domain.member.member.entity;

import com.example.infinite.domain.member.member.enums.MemberRole;
import com.example.infinite.domain.member.member.enums.MemberStatus;
import com.example.infinite.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@Table(name = "members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE members SET deleted_at = current_timestamp WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MemberStatus status;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;



    // 1. private 생성자 — 모든 필수 필드를 강제
    private Member(String email, String password, String nickname,
                   String phoneNumber) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.phoneNumber = phoneNumber;
        this.role = MemberRole.USER;
        this.status = MemberStatus.ACTIVE;
    }

    // 2. 정적 팩토리 메서드 — 회원가입 시 기본값 캡슐화
    public static Member createNewMember(String email, String password,
                                         String nickname, String phoneNumber) {
        return new Member(email, password, nickname, phoneNumber);
    }

    // 내 정보 수정에서 사용하는 프로필성 필드 변경 메서드다.
    public void updateProfile(String nickname, String phoneNumber, String profileImageUrl) {
        this.nickname = nickname;
        this.phoneNumber = phoneNumber;
        this.profileImageUrl = profileImageUrl;
    }

    // 비밀번호는 서비스 계층에서 암호화한 값을 받아 교체한다.
    public void changePassword(String password) {
        this.password = password;
    }

    // 로그인 식별자(email) 변경 시 사용한다.
    public void changeEmail(String email) {
        this.email = email;
    }

    // SUPER_ADMIN 전용 역할 승격에 사용한다.
    public void changeRole(MemberRole role) {
        this.role = role;
    }

    // 관리자 판단에 따른 상태 전환에 사용한다.
    public void changeStatus(MemberStatus status) {
        this.status = status;
    }
}
