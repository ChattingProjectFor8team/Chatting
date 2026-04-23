package com.example.infinite.domain.artistcontent.post.fanletter.service;

import com.example.infinite.global.common.constant.CacheNames;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class FanLetterSpecialLikeCacheInvalidationListener {

    private final CacheManager cacheManager;

    /**
     * special-like는 Reaction 원본 기반 읽기 조립값이라
     * 실제 좋아요 토글 트랜잭션이 커밋된 뒤에만 base cache를 비운다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(FanLetterSpecialLikeCacheInvalidationEvent event) {
        Cache listCache = cacheManager.getCache(CacheNames.FAN_LETTER_LIST_BASE);
        Cache detailCache = cacheManager.getCache(CacheNames.FAN_LETTER_DETAIL_BASE);

        if (listCache != null) {
            // 목록은 cursor 키가 갈라져 있어 special-like 변경 시 전체 목록 base cache를 비운다.
            listCache.clear();
        }
        if (detailCache != null) {
            detailCache.evict(event.artistId() + ":" + event.fanLetterId());
        }
    }
}
