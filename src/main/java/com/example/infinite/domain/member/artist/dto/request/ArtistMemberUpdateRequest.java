package com.example.infinite.domain.member.artist.dto.request;

import com.example.infinite.domain.member.member.enums.MemberStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

// 아티스트 멤버 수정은 부분 변경을 허용한다.
@Schema(description = "아티스트 멤버 수정 요청")
public record ArtistMemberUpdateRequest(
        @Schema(description = "수정할 활동명", example = "JEONGHAN")
        @Size(max = 100)
        String stageName,

        @Schema(description = "수정할 프로필 이미지 URL", example = "https://cdn.infinite.com/artists/seventeen/jeonghan-v2.jpg")
        @Size(max = 500)
        String profileImageUrl,

        @Schema(description = "멤버 상태", example = "ACTIVE")
        MemberStatus status,

        @Schema(description = "수정할 노출 순서", example = "3")
        @Positive
        Integer sortOrder
) {
}
