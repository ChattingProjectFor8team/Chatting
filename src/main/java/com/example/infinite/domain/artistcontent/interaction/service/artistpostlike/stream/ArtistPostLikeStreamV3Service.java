package com.example.infinite.domain.artistcontent.interaction.service.artistpostlike.stream;

import com.example.infinite.domain.artistcontent.interaction.dto.response.ArtistPostLikeQueuedResponse;
import com.example.infinite.domain.artistcontent.interaction.error.InteractionErrorCode;
import com.example.infinite.domain.artistcontent.interaction.error.InteractionException;
import com.example.infinite.global.lock.LockException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArtistPostLikeStreamV3Service {

    private final ArtistPostLikeRedissonV3LockedService artistPostLikeRedissonV3LockedService;

    public ArtistPostLikeQueuedResponse queue(Long memberId, Long artistId, Long artistPostId) {
        try {
            return artistPostLikeRedissonV3LockedService.queueWithLock(memberId, artistId, artistPostId);
        } catch (LockException e) {
            throw new InteractionException(InteractionErrorCode.LIKE_REQUEST_IN_PROGRESS);
        }
    }
}
