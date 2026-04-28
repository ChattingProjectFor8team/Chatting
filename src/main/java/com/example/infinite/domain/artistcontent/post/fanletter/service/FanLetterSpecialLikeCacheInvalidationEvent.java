package com.example.infinite.domain.artistcontent.post.fanletter.service;

/**
 * FanLetter special-like 표시값을 다시 계산해야 할 때 발행하는 이벤트다.
 *
 * special-like는 FanLetter 엔티티 필드가 아니라
 * "아티스트 멤버의 좋아요 존재 여부"를 읽기 시점에 조립하므로
 * 좋아요 토글 후 base cache를 함께 비워야 한다.
 */
public record FanLetterSpecialLikeCacheInvalidationEvent(
        Long artistId,
        Long fanLetterId
) {
}
