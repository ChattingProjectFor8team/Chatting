package com.example.infinite.domain.raffle.controller;

import com.example.infinite.domain.raffle.dto.CreateRaffleRequest;
import com.example.infinite.domain.raffle.dto.RaffleResponse;
import com.example.infinite.domain.raffle.service.RaffleService;
import com.example.infinite.global.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/artists/{artistId}/raffles")
@RequiredArgsConstructor
public class RaffleAdminController {

    private final RaffleService raffleService;

    @PostMapping
    public ResponseEntity<ApiResponse<RaffleResponse>> createRaffle(
            @PathVariable Long artistId,
            @Valid @RequestBody CreateRaffleRequest request) {
        RaffleResponse response = raffleService.createRaffle(artistId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PatchMapping("/{raffleId}/start")
    public ResponseEntity<ApiResponse<RaffleResponse>> startRaffle(
            @PathVariable Long artistId,
            @PathVariable Long raffleId) {
        RaffleResponse response = raffleService.startRaffle(artistId, raffleId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{raffleId}/cancel")
    public ResponseEntity<ApiResponse<RaffleResponse>> cancelRaffle(
            @PathVariable Long artistId,
            @PathVariable Long raffleId) {
        RaffleResponse response = raffleService.cancelRaffle(artistId, raffleId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}