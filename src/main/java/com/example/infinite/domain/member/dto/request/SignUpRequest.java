package com.example.infinite.domain.member.dto.request;

public record SignUpRequest(String email, String password, String phoneNumber, String nickname) {}