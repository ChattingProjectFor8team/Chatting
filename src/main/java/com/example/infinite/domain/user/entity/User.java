package com.example.infinite.domain.user.entity;

import com.example.infinite.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE users SET deleted_at = current_timestamp WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String phoneNumber;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private String subscriptionStatus;

    private User(String email, String password, String phoneNumber, UserRole role,
                 String status, String nickname, String subscriptionStatus) {
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.role = role;
        this.status = status;
        this.nickname = nickname;
        this.subscriptionStatus = subscriptionStatus;
    }

    // 2. 정적 팩토리 메서드 (회원가입 등 객체 생성 시 사용)
    public static User createNewUser(String email, String password, String phoneNumber, String nickname) {
        return new User(
                email,
                password,
                phoneNumber,
                UserRole.USER,      // 초기 가입 시 기본 권한 설정
                "ACTIVE",           // 초기 상태
                nickname,
                "NONE"              // 초기 구독 상태
        );
    }
}






