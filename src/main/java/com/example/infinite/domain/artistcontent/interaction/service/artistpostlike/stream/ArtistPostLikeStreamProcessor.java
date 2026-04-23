package com.example.infinite.domain.artistcontent.interaction.service.artistpostlike.stream;

import com.example.infinite.domain.artistcontent.interaction.entity.Reaction;
import com.example.infinite.domain.artistcontent.interaction.enums.ReactionType;
import com.example.infinite.domain.artistcontent.interaction.repository.InteractionRepository;
import com.example.infinite.domain.artistcontent.post.artistpost.support.ArtistPostReader;
import com.example.infinite.domain.artistcontent.post.error.ArtistContentException;
import com.example.infinite.domain.artistcontent.post.artistpost.service.likecount.ArtistPostLikeDeltaEvent;
import com.example.infinite.domain.artistcontent.post.enums.PostType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArtistPostLikeStreamProcessor {

    private final InteractionRepository interactionRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ArtistPostLikePendingStateRepository pendingStateRepository;
    private final ArtistPostReader artistPostReader;

    /**
     * stream consumer가 실제 DB side-effect를 수행하는 곳이다.
     *
     * 요청 경로에서는 command만 적재하고,
     * 진짜 Reaction insert/delete 및 likeCount delta 발행은 여기서 일어난다.
     */
    @Transactional
    public void process(ArtistPostLikeStreamCommand command) {
        try {
            // queue 이후 게시글이 삭제될 수 있으므로, consumer에서도 대상 post를 다시 확인한다.
            // 이 검증이 없으면 soft delete 된 post에 orphan reaction row가 생길 수 있다.
            artistPostReader.findByIdAndArtistIdOrThrow(command.artistPostId(), command.artistId());
        } catch (ArtistContentException e) {
            log.warn("ArtistPost like stream command skipped: target post not found, requestId={}, artistPostId={}",
                    command.requestId(), command.artistPostId());
            pendingStateRepository.clearPendingStateIfVersionMatches(
                    command.artistPostId(),
                    command.memberId(),
                    command.pendingVersion()
            );
            return;
        }

        // desired state 명령은 재시도돼도 "현재 DB 상태와 다른 경우에만" side effect를 일으켜야 한다.
        // 조회를 한 번만 수행해 실제 insert/delete가 일어날 때만 delta를 발행한다.
        Optional<Reaction> existingReaction = interactionRepository.findByTargetTypeAndTargetIdAndMemberIdAndReactionType(
                PostType.ARTIST_POST,
                command.artistPostId(),
                command.memberId(),
                ReactionType.LIKE
        );

        if (command.desiredReacted() && existingReaction.isEmpty()) {
            interactionRepository.save(Reaction.create(
                    PostType.ARTIST_POST,
                    command.artistPostId(),
                    command.memberId(),
                    ReactionType.LIKE
            ));
            applicationEventPublisher.publishEvent(new ArtistPostLikeDeltaEvent(command.artistPostId(), 1L));
        } else if (!command.desiredReacted() && existingReaction.isPresent()) {
            interactionRepository.delete(existingReaction.get());
            applicationEventPublisher.publishEvent(new ArtistPostLikeDeltaEvent(command.artistPostId(), -1L));
        } else {
            // 재처리나 중복 명령이 와도 desired state와 현재 DB 상태가 같으면 조용히 no-op 처리한다.
            log.debug("ArtistPost like stream command no-op: requestId={}, desiredReacted={}",
                    command.requestId(), command.desiredReacted());
        }

        pendingStateRepository.clearPendingStateIfVersionMatches(
                command.artistPostId(),
                command.memberId(),
                command.pendingVersion()
        );
    }
}
