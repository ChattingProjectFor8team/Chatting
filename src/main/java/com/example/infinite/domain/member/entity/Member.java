package com.example.infinite.domain.member.entity;

import com.example.infinite.domain.member.enums.MemberRole;
import com.example.infinite.domain.member.enums.MemberStatus;
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

    @Column(name = "phone_number", unique = true, length = 30)
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


    public static Member createNewMember(String email, String password, String nickname, String phoneNumber, MemberRole role) {
        Member member = new Member();
        member.email = email;
        member.password = password;
        member.nickname = nickname;
        member.phoneNumber = phoneNumber;
        member.role = role;
        member.status = MemberStatus.ACTIVE;
        return member;
    }

}
