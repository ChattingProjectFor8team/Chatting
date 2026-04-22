package com.example.infinite.domain.artistcontent.comment.service.artistpoststream;

import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

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
