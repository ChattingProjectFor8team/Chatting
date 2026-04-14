package com.example.infinite.domain.Raffle.Service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ReservoirSamplerUnitTest {

    // Redis 의존성 없이 순수 로직만 테스트
    private final ReservoirSampler sampler = new ReservoirSampler(null);

    @Test
    @DisplayName("첫 번째 참여자는 무조건 후보로 선정된다")
    void firstParticipantAlwaysSelected() {
        for (int i = 0; i < 1_000; i++) {
            assertThat(sampler.shouldReplace(1)).isTrue();
        }
    }

    @Test
    @DisplayName("count가 0 이하이면 예외가 발생한다")
    void invalidCountThrowsException() {
        assertThatThrownBy(() -> sampler.shouldReplace(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sampler.shouldReplace(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Reservoir Sampling은 모든 참여자에게 균등한 확률(1/n)을 보장한다 (n=100)")
    void uniformDistribution_n100() {
        int n = 100;
        int simulations = 100_000;
        int[] selectionCount = new int[n];

        for (int sim = 0; sim < simulations; sim++) {
            int candidate = 0;
            for (int i = 1; i < n; i++) {
                if (sampler.shouldReplace(i + 1)) {
                    candidate = i;
                }
            }
            selectionCount[candidate]++;
        }

        double expected = 1.0 / n;
        double tolerance = 0.02;

        for (int i = 0; i < n; i++) {
            double actual = (double) selectionCount[i] / simulations;
            assertThat(actual)
                    .as("참여자 %d의 선정 확률: 기대=%.4f, 실제=%.4f", i, expected, actual)
                    .isBetween(expected - tolerance, expected + tolerance);
        }
    }

    @Test
    @DisplayName("참여자 수가 늘어나도 균등 분포가 유지된다 (n=1000)")
    void uniformDistribution_n1000() {
        int n = 1_000;
        int simulations = 100_000;
        int[] selectionCount = new int[n];

        for (int sim = 0; sim < simulations; sim++) {
            int candidate = 0;
            for (int i = 1; i < n; i++) {
                if (sampler.shouldReplace(i + 1)) {
                    candidate = i;
                }
            }
            selectionCount[candidate]++;
        }

        double expected = 1.0 / n;
        double tolerance = 0.005;

        int[] sampleIndices = {0, 99, 250, 499, 500, 750, 900, 950, 998, 999};
        for (int idx : sampleIndices) {
            double actual = (double) selectionCount[idx] / simulations;
            assertThat(actual)
                    .as("참여자 %d의 선정 확률 (n=%d)", idx, n)
                    .isBetween(expected - tolerance, expected + tolerance);
        }
    }
}
