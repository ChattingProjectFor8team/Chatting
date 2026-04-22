package com.example.infinite.domain.artistcontent.interaction.service.fanpostlike;

import com.example.infinite.domain.artistcontent.interaction.dto.response.InteractionResponse;
import com.example.infinite.domain.artistcontent.interaction.error.InteractionErrorCode;
import com.example.infinite.domain.artistcontent.interaction.error.InteractionException;
import com.example.infinite.global.lock.LockException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FanPostLikeRedissonService {

    private final FanPostLikeRedissonLockedService fanPostLikeRedissonLockedService;

    /**
     * FanPost는 ArtistPost처럼 burst를 stream으로 흡수할 필요까진 없다고 보고,
     * 운영 경로를 Redisson + 즉시 DB 반영으로 단순하게 유지한다.
     */
    public InteractionResponse toggle(Long memberId, Long artistId, Long fanPostId) {
        try {
            return fanPostLikeRedissonLockedService.toggleWithLock(memberId, artistId, fanPostId);
        } catch (LockException e) {
            throw new InteractionException(InteractionErrorCode.LIKE_REQUEST_IN_PROGRESS);
        }
    }
}
