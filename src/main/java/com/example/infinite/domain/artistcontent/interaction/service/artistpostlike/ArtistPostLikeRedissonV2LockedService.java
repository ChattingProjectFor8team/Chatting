package com.example.infinite.domain.artistcontent.interaction.service.artistpostlike;

import com.example.infinite.domain.artistcontent.interaction.dto.response.InteractionResponse;
import com.example.infinite.global.lock.RedisLock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ArtistPostLikeRedissonV2LockedService {

    private final ArtistPostLikeCoreService artistPostLikeCoreService;

    /**
     * V2 실사용 락 구간.
     *
     * 왜 별도 클래스로 분리했는가?
     * - @RedisLock는 AOP 기반이라 "다른 스프링 빈을 통해" 호출돼야 적용된다.
     * - 같은 클래스 내부 메서드 호출(self-invocation)로는 프록시를 거치지 않아 락이 안 걸릴 수 있다.
     *
     * 왜 키가 memberId + artistPostId 인가?
     * - 같은 유저의 중복 토글만 직렬화하면 충분하다.
     * - 유저 1만 명이 동시에 같은 글을 눌러도 락 키는 전부 달라 병렬 처리된다.
     */
    @RedisLock(
            key = "'artist-post:like:' + #artistPostId + ':member:' + #memberId",
            waitTime = 700,
            leaseTime = 3000,
            timeUnit = TimeUnit.MILLISECONDS
    )
    public InteractionResponse toggleWithLock(Long memberId, Long artistId, Long artistPostId) {
        // 락을 잡은 상태에서만 core 비즈니스를 실행한다.
        // 락 구현은 Redisson/AOP에 맡기고, 실제 토글 로직은 core service가 담당한다.
        return artistPostLikeCoreService.toggle(memberId, artistId, artistPostId);
    }
}
