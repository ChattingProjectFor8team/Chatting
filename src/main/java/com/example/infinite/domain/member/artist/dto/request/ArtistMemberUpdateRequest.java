package com.example.infinite.domain.member.artist.dto.request;

import com.example.infinite.domain.member.member.enums.MemberStatus;
import jakarta.validation.constraints.Size;

// 아티스트 멤버 수정은 부분 변경을 허용한다.
public record ArtistMemberUpdateRequest(
        @Size(max = 100)
        String stageName,

        @Size(max = 500)
        String profileImageUrl,

        MemberStatus status,

        Integer sortOrder
) {
}
