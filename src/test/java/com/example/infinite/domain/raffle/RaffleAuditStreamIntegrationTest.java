package com.example.infinite.domain.raffle;

import com.example.infinite.domain.raffle.service.RaffleAuditConsumer;
import com.example.infinite.domain.raffle.service.RaffleAuditLogger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Testcontainers
class RaffleAuditStreamIntegrationTest {

    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private RaffleAuditLogger auditLogger;

    @Autowired
    private RaffleAuditConsumer auditConsumer;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void setUp() {
        stringRedisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    @DisplayName("XADD로 발행한 감사 로그를 Consumer가 읽어온다")
    void publishAndConsume() {
        long raffleId = 1L;
        String streamKey = "raffle:1:audit-log";

        auditLogger.log(raffleId, 0, 100L, 1, true);
        auditConsumer.createConsumerGroupIfNotExists(streamKey);

        auditLogger.log(raffleId, 0, 101L, 2, false);
        auditLogger.log(raffleId, 0, 102L, 3, true);

        int consumed = auditConsumer.consume(streamKey);
        assertThat(consumed).isEqualTo(3);
    }

    @Test
    @DisplayName("Consumer Group이 이미 존재해도 에러 없이 무시된다")
    void duplicateConsumerGroupCreation() {
        long raffleId = 2L;
        String streamKey = "raffle:2:audit-log";

        auditLogger.log(raffleId, 0, 200L, 1, false);

        auditConsumer.createConsumerGroupIfNotExists(streamKey);
        auditConsumer.createConsumerGroupIfNotExists(streamKey);
    }

    @Test
    @DisplayName("소비한 메시지는 재소비되지 않는다 (ACK 확인)")
    void acknowledgedMessagesNotReconsumed() {
        long raffleId = 3L;
        String streamKey = "raffle:3:audit-log";

        auditLogger.log(raffleId, 0, 300L, 1, false);
        auditConsumer.createConsumerGroupIfNotExists(streamKey);

        int first = auditConsumer.consume(streamKey);
        assertThat(first).isEqualTo(1);

        int second = auditConsumer.consume(streamKey);
        assertThat(second).isEqualTo(0);
    }

    @Test
    @DisplayName("스트림 삭제 후 키가 존재하지 않는다")
    void deleteStream() {
        long raffleId = 4L;
        String streamKey = "raffle:4:audit-log";

        auditLogger.log(raffleId, 0, 400L, 1, false);
        assertThat(stringRedisTemplate.hasKey(streamKey)).isTrue();

        auditConsumer.deleteStream(raffleId);
        assertThat(stringRedisTemplate.hasKey(streamKey)).isFalse();
    }

    @Test
    @DisplayName("감사 로그 발행 실패가 예외를 던지지 않는다")
    void logFailureDoesNotThrow() {
        assertThatCode(() -> auditLogger.log(-1L, -1, -1L, -1, false))
                .doesNotThrowAnyException();
    }
}