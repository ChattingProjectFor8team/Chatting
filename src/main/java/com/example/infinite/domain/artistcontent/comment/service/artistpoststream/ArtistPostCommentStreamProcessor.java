package com.example.infinite.domain.artistcontent.comment.service.artistpoststream;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArtistPostCommentStreamProcessor {

    private final ArtistPostCommentCoreService artistPostCommentCoreService;
    private final ArtistPostCommentThreadLockedService artistPostCommentThreadLockedService;

    /**
     * 댓글 command type에 따라 어떤 처리 경로를 탈지 결정하는 dispatcher다.
     *
     * 규칙:
     * - root create: 락 불필요
     * - reply create: root thread lock 필요
     * - delete: 대상 comment가 root인지 reply인지 확인 후 root thread lock 필요
     */
    public void process(ArtistPostCommentStreamCommand command) {
        switch (command.commandType()) {
            case CREATE_ROOT -> artistPostCommentCoreService.createRoot(command);
            case CREATE_REPLY -> artistPostCommentThreadLockedService.createReplyWithLock(command, command.parentCommentId());
            case DELETE -> {
                Long rootCommentId = artistPostCommentCoreService.resolveRootCommentIdForDelete(
                        command.artistPostId(),
                        command.commentId()
                );
                if (rootCommentId == null) {
                    return;
                }
                artistPostCommentThreadLockedService.deleteWithLock(command, rootCommentId);
            }
        }
    }
}
