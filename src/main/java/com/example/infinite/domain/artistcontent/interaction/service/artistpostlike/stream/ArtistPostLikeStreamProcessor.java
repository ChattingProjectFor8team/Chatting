package com.example.infinite.domain.artistcontent.interaction.service.artistpostlike.stream;

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

        // V3 consumer는 burst 처리량이 중요하므로
        // "조회 후 insert/delete" 대신 조건부 DML 한 번으로 멱등성을 판정한다.
        //
        // 이번 수정의 배경:
        // - 테스트 시나리오상 "BTS 글 알림 후 수많은 팬이 동시에 진입"하는 상황을 가정한다
        // - 이때 desired=true 명령이 많이 몰리면
        //   select -> save 구조는 처리량이 낮고, 재시도 타이밍에 unique key 충돌도 날 수 있다
        // - 그래서 DB가 직접 "있으면 무시 / 없으면 삭제 0건"을 판단하게 바꿨다
        //
        // 핵심은 "예외 없이 멱등 no-op 처리"다.
        // 같은 명령이 다시 와도 count delta를 두 번 발행하지 않도록
        // 실제 row 수가 바뀐 경우에만 delta 이벤트를 내보낸다.
        if (command.desiredReacted()) {
            int insertedRowCount = interactionRepository.insertIgnore(
                    PostType.ARTIST_POST.name(),
                    command.artistPostId(),
                    command.memberId(),
                    ReactionType.LIKE.name()
            );
            if (insertedRowCount > 0) {
                applicationEventPublisher.publishEvent(new ArtistPostLikeDeltaEvent(command.artistPostId(), 1L));
            } else {
                log.debug("ArtistPost like stream command no-op: requestId={}, desiredReacted={}",
                        command.requestId(), command.desiredReacted());
            }
        } else {
            int deletedRowCount = interactionRepository.deleteIfExists(
                    PostType.ARTIST_POST,
                    command.artistPostId(),
                    command.memberId(),
                    ReactionType.LIKE
            );
            if (deletedRowCount > 0) {
                applicationEventPublisher.publishEvent(new ArtistPostLikeDeltaEvent(command.artistPostId(), -1L));
            } else {
                log.debug("ArtistPost like stream command no-op: requestId={}, desiredReacted={}",
                        command.requestId(), command.desiredReacted());
            }
        }

        // pending state 정리는 마지막에 한다.
        // 그래야 DB 반영 성공/스킵/no-op 여부가 결정된 뒤에만
        // "이 명령 버전까지는 처리 완료"라고 요청 경로가 안전하게 판단할 수 있다.
        pendingStateRepository.clearPendingStateIfVersionMatches(
                command.artistPostId(),
                command.memberId(),
                command.pendingVersion()
        );
    }
}
