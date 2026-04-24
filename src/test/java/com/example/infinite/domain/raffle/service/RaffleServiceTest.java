package com.example.infinite.domain.raffle.service;

import com.example.infinite.domain.raffle.dto.RaffleDetailResponse;
import com.example.infinite.domain.raffle.entity.Raffle;
import com.example.infinite.domain.raffle.enums.EntryCondition;
import com.example.infinite.domain.raffle.enums.RewardType;
import com.example.infinite.domain.raffle.repository.RaffleEntryRepository;
import com.example.infinite.domain.raffle.repository.RaffleRepository;
import com.example.infinite.domain.raffle.repository.RaffleSlotRepository;
import com.example.infinite.domain.raffle.repository.RaffleSlotWinnerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RaffleService 단위 테스트")
class RaffleServiceTest {

    @Mock RaffleRepository raffleRepository;
    @Mock RaffleSlotRepository raffleSlotRepository;
    @Mock RaffleSlotWinnerRepository raffleSlotWinnerRepository;
    @Mock RaffleEntryRepository raffleEntryRepository;
    @Mock ReservoirSampler reservoirSampler;
    @Mock RaffleSchedulerService schedulerService;
    @Mock StringRedisTemplate stringRedisTemplate;
    @Mock RaffleNotificationService raffleNotificationService;

    @InjectMocks RaffleService raffleService;

    @Nested
    @DisplayName("getRaffleDetail()")
    class GetRaffleDetailTest {

        @Test
        @DisplayName("비로그인 사용자(userId=null)의 래플 상세 조회 시 entered=false로 정상 응답한다")
        void getRaffleDetail_withNullUserId_returnsEnteredFalse() {
            Long artistId = 10L;
            Long raffleId = 1L;

            Raffle raffle = Raffle.builder()
                    .artistId(artistId)
                    .title("팬미팅 초대권")
                    .entryCondition(EntryCondition.ALL)
                    .rewardType(RewardType.MEMBERSHIP_EXTENSION)
                    .totalWinners(1)
                    .durationMinutes(60)
                    .build();

            try {
                var idField = Raffle.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(raffle, raffleId);
            } catch (Exception e) {
                throw new RuntimeException("테스트 Raffle ID 설정 실패", e);
            }

            when(raffleRepository.findById(raffleId)).thenReturn(Optional.of(raffle));

            RaffleDetailResponse response = raffleService.getRaffleDetail(artistId, raffleId, null);

            assertThat(response).isNotNull();
            assertThat(response.entered()).isFalse();
            assertThat(response.title()).isEqualTo("팬미팅 초대권");

            verify(raffleEntryRepository, never()).existsByRaffleIdAndUserId(anyLong(), anyLong());
        }
    }
}
