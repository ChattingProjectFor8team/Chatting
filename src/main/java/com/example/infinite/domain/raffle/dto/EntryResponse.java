package com.example.infinite.domain.raffle.dto;

/**
 * 응모 결과 비대칭 응답.
 * 당첨 여부는 슬롯 종료 시점에 확정되므로, 응모 시점에는 "응모 완료" 메시지만 반환한다.
 */
public record EntryResponse(
        String message
) {
    public static EntryResponse success() {
        return new EntryResponse("응모가 완료되었습니다. 결과는 래플 종료 후 확인할 수 있습니다.");
    }
}