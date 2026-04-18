package com.example.infinite.domain.member.artist.dto.response;

import com.example.infinite.domain.member.member.enums.MemberStatus;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public record ArtistResponse(
        Long artistId,
        String name,
        String slug,
        String profileImageUrl,
        String coverImageUrl,
        String intro,
        MemberStatus artistStatus,
        LocalDateTime createdAt,
        List<ArtistMemberResponse> artistMembers
) {
    public static ArtistResponse from(List<ArtistDetailRow> rows) {
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("아티스트 상세 응답을 만들 데이터가 없습니다.");
        }

        ArtistDetailRow first = rows.get(0);
        List<ArtistMemberResponse> artistMembers = rows.stream()
                .filter(row -> row.artistMemberId() != null)
                .map(ArtistMemberResponse::from)
                .toList();

        return new ArtistResponse(
                first.artistId(),
                first.name(),
                first.slug(),
                first.profileImageUrl(),
                first.coverImageUrl(),
                first.intro(),
                first.artistStatus(),
                first.createdAt(),
                Collections.unmodifiableList(artistMembers)
        );
    }
}
