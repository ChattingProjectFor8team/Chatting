package com.example.infinite.domain.member.member.dto.request;

import com.example.infinite.domain.member.member.enums.MemberRole;
import jakarta.validation.constraints.NotNull;

// 관리자 역할 승격 요청 전용 DTO다.
public record UpdateMemberRoleRequest(
        @NotNull MemberRole role
) {
}
