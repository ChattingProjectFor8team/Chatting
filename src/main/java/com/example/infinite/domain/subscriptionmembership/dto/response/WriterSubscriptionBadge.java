package com.example.infinite.domain.subscriptionmembership.dto.response;

/**
 * 팬포스트·팬레터·댓글 목록에서 작성자 닉네임 옆에 표시할 구독 뱃지 정보.
 * artistId 기준으로 해당 작성자(writerId)가 보유한 구독 종류를 담는다.
 * 응답 Map에 포함되지 않은 writerId는 양쪽 모두 false로 간주한다.
 */
public record WriterSubscriptionBadge(
        Long writerId,
        // 해당 작성자가 이 아티스트의 팬 멤버십을 보유 중인지 여부
        boolean fanMembershipSubscribed,
        // 해당 작성자가 이 아티스트의 DM 구독권을 보유 중인지 여부
        boolean dmSubscribed
) {
    public static WriterSubscriptionBadge empty(Long writerId) {
        return new WriterSubscriptionBadge(writerId, false, false);
    }
}
