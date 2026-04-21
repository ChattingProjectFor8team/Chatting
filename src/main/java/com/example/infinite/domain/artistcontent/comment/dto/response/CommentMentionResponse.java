package com.example.infinite.domain.artistcontent.comment.dto.response;

import com.example.infinite.domain.artistcontent.comment.entity.CommentMention;

public record CommentMentionResponse(
        Long memberId,
        String nickname,
        String profileImageUrl
) {
    public static CommentMentionResponse from(CommentMention commentMention) {
        // 프론트가 멘션 대상 프로필 정도는 바로 쓸 수 있게 최소 정보만 평탄화한다.
        return new CommentMentionResponse(
                commentMention.getMentionedMember().getId(),
                commentMention.getMentionedMember().getNickname(),
                commentMention.getMentionedMember().getProfileImageUrl()
        );
    }
}
