package com.example.infinite.domain.artistcontent.interaction.service.artistpostlike.stream;

import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;

import java.time.Instant;
import java.util.Map;

public record ArtistPostLikeStreamCommand(
        String requestId,
        Long artistId,
        Long artistPostId,
        Long memberId,
        boolean desiredReacted,
        long pendingVersion,
        String queuedAt
) {
    public static ArtistPostLikeStreamCommand create(
            String requestId,
            Long artistId,
            Long artistPostId,
            Long memberId,
            boolean desiredReacted,
            long pendingVersion
    ) {
        return new ArtistPostLikeStreamCommand(
                requestId,
                artistId,
                artistPostId,
                memberId,
                desiredReacted,
                pendingVersion,
                Instant.now().toString()
        );
    }

    public MapRecord<String, String, String> toRecord(String streamKey) {
        return StreamRecords.newRecord()
                .in(streamKey)
                .ofMap(Map.of(
                        "requestId", requestId,
                        "artistId", artistId.toString(),
                        "artistPostId", artistPostId.toString(),
                        "memberId", memberId.toString(),
                        "desiredReacted", Boolean.toString(desiredReacted),
                        "pendingVersion", Long.toString(pendingVersion),
                        "queuedAt", queuedAt
                ));
    }

    public static ArtistPostLikeStreamCommand from(MapRecord<String, Object, Object> record) {
        Map<Object, Object> fields = record.getValue();
        return new ArtistPostLikeStreamCommand(
                fields.get("requestId").toString(),
                Long.valueOf(fields.get("artistId").toString()),
                Long.valueOf(fields.get("artistPostId").toString()),
                Long.valueOf(fields.get("memberId").toString()),
                Boolean.parseBoolean(fields.get("desiredReacted").toString()),
                Long.parseLong(fields.get("pendingVersion").toString()),
                fields.get("queuedAt").toString()
        );
    }
}
