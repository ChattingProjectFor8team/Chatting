package com.example.infinite.domain.artistcontent.interaction.service.fanletterlike;

import com.example.infinite.domain.artistcontent.interaction.dto.response.InteractionResponse;
import com.example.infinite.global.lock.RedisLock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class FanLetterLikeRedissonLockedService {

    private final FanLetterLikeCoreService fanLetterLikeCoreService;

    @RedisLock(
            key = "'fan-letter:like:' + #fanLetterId + ':member:' + #memberId",
            waitTime = 700,
            leaseTime = 3000,
            timeUnit = TimeUnit.MILLISECONDS
    )
    public InteractionResponse toggleWithLock(Long memberId, Long artistId, Long fanLetterId) {
        return fanLetterLikeCoreService.toggle(memberId, artistId, fanLetterId);
    }
}
