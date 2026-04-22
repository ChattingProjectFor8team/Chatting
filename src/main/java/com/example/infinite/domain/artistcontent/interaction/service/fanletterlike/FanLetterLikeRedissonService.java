package com.example.infinite.domain.artistcontent.interaction.service.fanletterlike;

import com.example.infinite.domain.artistcontent.interaction.dto.response.InteractionResponse;
import com.example.infinite.domain.artistcontent.interaction.error.InteractionErrorCode;
import com.example.infinite.domain.artistcontent.interaction.error.InteractionException;
import com.example.infinite.global.lock.LockException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FanLetterLikeRedissonService {

    private final FanLetterLikeRedissonLockedService fanLetterLikeRedissonLockedService;

    /**
     * FanLetter도 즉시 처리 경로를 유지한다.
     * 과한 비동기화보다 "같은 유저 중복 토글만 막고 count는 atomic update"가 더 적합한 영역이다.
     */
    public InteractionResponse toggle(Long memberId, Long artistId, Long fanLetterId) {
        try {
            return fanLetterLikeRedissonLockedService.toggleWithLock(memberId, artistId, fanLetterId);
        } catch (LockException e) {
            throw new InteractionException(InteractionErrorCode.LIKE_REQUEST_IN_PROGRESS);
        }
    }
}
