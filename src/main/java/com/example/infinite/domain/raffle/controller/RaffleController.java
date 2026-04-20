package com.example.infinite.domain.raffle.controller;

import com.example.infinite.domain.raffle.dto.EntryResponse;
import com.example.infinite.domain.raffle.dto.MyEntryResultResponse;
import com.example.infinite.domain.raffle.dto.MyRaffleEntryResponse;
import com.example.infinite.domain.raffle.dto.RaffleDetailResponse;
import com.example.infinite.domain.raffle.dto.RaffleResponse;
import com.example.infinite.domain.raffle.service.RaffleEntryService;
import com.example.infinite.domain.raffle.service.RaffleService;
import com.example.infinite.global.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RaffleController {

    private final RaffleEntryService raffleEntryService;
    private final RaffleService raffleService;

    @PostMapping("/api/v1/artists/{artistId}/raffles/{raffleId}/entries")
    public ResponseEntity<ApiResponse<EntryResponse>> enter(
            @PathVariable Long artistId,
            @PathVariable Long raffleId,
            @RequestHeader("X-User-Id") Long userId) {
        EntryResponse response = raffleEntryService.enter(artistId, raffleId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/api/v1/artists/{artistId}/raffles")
    public ResponseEntity<ApiResponse<List<RaffleResponse>>> getRaffles(
            @PathVariable Long artistId) {
        List<RaffleResponse> response = raffleService.getRafflesByArtist(artistId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/api/v1/artists/{artistId}/raffles/{raffleId}")
    public ResponseEntity<ApiResponse<RaffleDetailResponse>> getRaffleDetail(
            @PathVariable Long artistId,
            @PathVariable Long raffleId,
            @RequestHeader("X-User-Id") Long userId) {
        RaffleDetailResponse response = raffleService.getRaffleDetail(artistId, raffleId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/api/v1/artists/{artistId}/raffles/{raffleId}/entries/me")
    public ResponseEntity<ApiResponse<MyEntryResultResponse>> getMyResult(
            @PathVariable Long artistId,
            @PathVariable Long raffleId,
            @RequestHeader("X-User-Id") Long userId) {
        MyEntryResultResponse response = raffleService.getMyResult(artistId, raffleId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/api/v1/users/me/raffle-entries")
    public ResponseEntity<ApiResponse<List<MyRaffleEntryResponse>>> getMyEntries(
            @RequestHeader("X-User-Id") Long userId) {
        List<MyRaffleEntryResponse> response = raffleService.getMyEntries(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}