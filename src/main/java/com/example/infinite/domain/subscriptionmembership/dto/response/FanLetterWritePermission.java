package com.example.infinite.domain.subscriptionmembership.dto.response;

/**
 * 팬레터 작성 가능 여부 응답.
 * 팬레터는 팬 멤버십 보유자만 작성할 수 있으므로 FanMembership 활성 여부로 판단한다.
 * 추후 조건이 추가되더라도 allowed 외 필드를 확장하는 방식으로 대응 가능하다.
 */
public record FanLetterWritePermission(
        Long artistId,
        Long memberId,
        // 팬 멤버십 활성 상태일 때만 true
        boolean allowed
) {}
