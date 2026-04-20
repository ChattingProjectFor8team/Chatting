package com.example.infinite.domain.member.member.dto.request;

import com.example.infinite.domain.member.member.enums.MemberStatus;
import jakarta.validation.constraints.NotNull;

// 관리자 상태 변경 요청 전용 DTO다.
public record UpdateMemberStatusRequest(
        @NotNull MemberStatus status
) {
}
