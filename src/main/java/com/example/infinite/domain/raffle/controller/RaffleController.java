package com.example.infinite.domain.raffle.controller;

import com.example.infinite.domain.raffle.dto.EntryResponse;
import com.example.infinite.domain.raffle.service.RaffleEntryService;
import com.example.infinite.global.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class RaffleController {

    private final RaffleEntryService raffleEntryService;

    @PostMapping("/api/v1/artists/{artistId}/raffles/{raffleId}/entries")
    public ResponseEntity<ApiResponse<EntryResponse>> enter(
            @PathVariable Long artistId,
            @PathVariable Long raffleId,
            @RequestHeader("X-User-Id") Long userId) {
        EntryResponse response = raffleEntryService.enter(artistId, raffleId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }
}