package com.example.infinite.domain.artistcontent.interaction.service.fanpostlike;

import com.example.infinite.domain.artistcontent.interaction.dto.response.InteractionResponse;
import com.example.infinite.global.lock.RedisLock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class FanPostLikeRedissonLockedService {

    private final FanPostLikeCoreService fanPostLikeCoreService;

    /**
     * FanPost 좋아요는 같은 member-target 조합만 직렬화한다.
     * 같은 글에 대한 다른 유저 요청까지 잠그지 않아 병렬성은 유지한다.
     */
    @RedisLock(
            key = "'fan-post:like:' + #fanPostId + ':member:' + #memberId",
            waitTime = 700,
            leaseTime = 3000,
            timeUnit = TimeUnit.MILLISECONDS
    )
    public InteractionResponse toggleWithLock(Long memberId, Long artistId, Long fanPostId) {
        return fanPostLikeCoreService.toggle(memberId, artistId, fanPostId);
    }
}
