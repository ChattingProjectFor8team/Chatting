package com.example.infinite.domain.artistcontent.interaction.service.artistpostlike.stream;

import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;

import java.time.Instant;
import java.util.Map;

/**
 * Redis Stream 에 넣는 ArtistPost 좋아요 명령 객체다.
 *
 * 왜 별도 record 로 분리했는가:
 * - HTTP 요청 파라미터와 Stream payload 형식은 수명이 다르다
 * - producer / consumer 가 같은 필드 이름 규약을 공유해야 한다
 * - 직렬화/역직렬화 규칙을 한 파일에 모아야 추후 필드 추가 시 덜 위험하다
 */
public record ArtistPostLikeStreamCommand(
        String requestId,
        Long artistId,
        Long artistPostId,
        Long memberId,
        boolean desiredReacted,
        long pendingVersion,
        String queuedAt
) {
    /**
     * command 생성 시점의 시간을 함께 넣어 두면
     * - consumer 지연 관찰
     * - 디버깅 시 "언제 큐에 들어왔는지" 추적
     * 에 도움이 된다.
     */
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

    /**
     * Spring Redis Stream API 는 Map 형태 payload 를 다루므로
     * record -> MapRecord 변환을 여기서 책임진다.
     */
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

    /**
     * consumer 쪽에서는 Redis 레코드를 다시 command 로 복원해
     * 일반 서비스 로직처럼 읽기 쉬운 형태로 처리한다.
     */
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
