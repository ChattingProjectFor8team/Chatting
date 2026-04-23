package com.example.infinite.domain.artistcontent.follow.dto.response;

// 토글 API는 최종 상태만 내려주면 프론트가 버튼 UI를 바로 갱신할 수 있다.
public record FollowResponse(
        Long artistMemberId,
        boolean followed
) {
    public static FollowResponse of(Long artistMemberId, boolean followed) {
        return new FollowResponse(artistMemberId, followed);
    }
}
