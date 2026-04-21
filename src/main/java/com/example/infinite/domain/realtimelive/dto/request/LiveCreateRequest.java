package com.example.infinite.domain.realtimelive.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LiveCreateRequest(
        @NotBlank String title,
        String description,
        String thumbnailUrl
) {
}
