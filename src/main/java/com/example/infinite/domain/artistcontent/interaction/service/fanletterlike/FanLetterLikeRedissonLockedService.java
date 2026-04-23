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

    /**
     * FanLetter 좋아요도 같은 member-target 조합만 직렬화한다.
     *
     * 학습 포인트:
     * - 락 키를 fanLetterId + memberId로 잘게 쪼개면
     * - "같은 유저의 중복 토글"만 막고
     * - 다른 유저의 같은 글 좋아요는 계속 병렬 처리할 수 있다.
     */
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
