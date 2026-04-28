package com.example.infinite.domain.artistcontent.comment.service.artistpoststream;

import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ArtistPost 댓글 비동기 처리용 Stream payload 객체다.
 *
 * 좋아요 stream command 와 비슷하지만 댓글은 명령 종류에 따라
 * parentCommentId / commentId / content 의 필요 여부가 달라서
 * null 가능 필드를 함께 가진다.
 */
public record ArtistPostCommentStreamCommand(
        String requestId,
        ArtistPostCommentCommandType commandType,
        Long artistId,
        Long artistPostId,
        Long memberId,
        Long parentCommentId,
        Long commentId,
        String content,
        String queuedAt
) {
    /**
     * queuedAt 은 "DB 반영 완료 시각"이 아니라 "큐에 들어간 시각"이다.
     * 지연 추적이나 dead-letter 분석 때 도움이 된다.
     */
    public static ArtistPostCommentStreamCommand create(
            String requestId,
            ArtistPostCommentCommandType commandType,
            Long artistId,
            Long artistPostId,
            Long memberId,
            Long parentCommentId,
            Long commentId,
            String content
    ) {
        return new ArtistPostCommentStreamCommand(
                requestId,
                commandType,
                artistId,
                artistPostId,
                memberId,
                parentCommentId,
                commentId,
                content,
                Instant.now().toString()
        );
    }

    /**
     * commandType 에 따라 nullable 필드가 있으므로
     * 값이 있는 필드만 Map 에 담아 Stream payload 를 가볍게 유지한다.
     */
    public MapRecord<String, String, String> toRecord(String streamKey) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("requestId", requestId);
        fields.put("commandType", commandType.name());
        fields.put("artistId", artistId.toString());
        fields.put("artistPostId", artistPostId.toString());
        fields.put("memberId", memberId.toString());
        fields.put("queuedAt", queuedAt);
        if (parentCommentId != null) {
            fields.put("parentCommentId", parentCommentId.toString());
        }
        if (commentId != null) {
            fields.put("commentId", commentId.toString());
        }
        if (content != null) {
            fields.put("content", content);
        }
        return StreamRecords.newRecord().in(streamKey).ofMap(fields);
    }

    /**
     * consumer 가 Redis 메시지를 읽은 뒤
     * 다시 도메인 친화적인 command 객체로 복원하는 진입점이다.
     */
    public static ArtistPostCommentStreamCommand from(MapRecord<String, Object, Object> record) {
        Map<Object, Object> fields = record.getValue();
        return new ArtistPostCommentStreamCommand(
                fields.get("requestId").toString(),
                ArtistPostCommentCommandType.valueOf(fields.get("commandType").toString()),
                Long.valueOf(fields.get("artistId").toString()),
                Long.valueOf(fields.get("artistPostId").toString()),
                Long.valueOf(fields.get("memberId").toString()),
                fields.get("parentCommentId") == null ? null : Long.valueOf(fields.get("parentCommentId").toString()),
                fields.get("commentId") == null ? null : Long.valueOf(fields.get("commentId").toString()),
                fields.get("content") == null ? null : fields.get("content").toString(),
                fields.get("queuedAt").toString()
        );
    }
}
