package com.example.infinite.domain.raffle.dto;

/**
 * STOMP /sub/user/{userId}/notifications 으로 전송되는 래플 결과 알림.
 */
public record RaffleNotification(
        String type,        // "RAFFLE_WIN" | "RAFFLE_LOSE"
        Long raffleId,
        String raffleTitle,
        String message
) {
    public static RaffleNotification win(Long raffleId, String title) {
        return new RaffleNotification(
                "RAFFLE_WIN", raffleId, title,
                "축하합니다! '" + title + "' 래플에 당첨되었습니다. DM 구독이 30일 연장됩니다."
        );
    }

    public static RaffleNotification lose(Long raffleId, String title) {
        return new RaffleNotification(
                "RAFFLE_LOSE", raffleId, title,
                "'" + title + "' 래플에 아쉽게 당첨되지 못했습니다. 다음 기회에 도전해주세요!"
        );
    }
}
