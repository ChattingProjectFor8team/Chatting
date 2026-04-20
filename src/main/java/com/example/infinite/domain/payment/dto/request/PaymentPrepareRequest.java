package com.example.infinite.domain.payment.dto.request;

import com.example.infinite.domain.payment.enums.JellyProduct;
import jakarta.validation.constraints.NotNull;

public record PaymentPrepareRequest(
        @NotNull JellyProduct jellyProduct
) {}
