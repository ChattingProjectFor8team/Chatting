package com.example.infinite.domain.user.dto;

public record SignUpRequest(String email, String password, String phoneNumber, String nickname) {}