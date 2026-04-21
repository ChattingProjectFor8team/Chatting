package com.example.infinite.domain.member.member.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class UpdateMemberMultipartRequest {

    @Size(min = 2, max = 50)
    private String nickname;

    @Pattern(regexp = "^$|^010-\\d{4}-\\d{4}$", message = "전화번호는 010-1234-5678 형식이어야 합니다.")
    @Size(max = 13)
    private String phoneNumber;

    @Size(max = 500)
    private String profileImageUrl;

    @Size(max = 500)
    private String coverImageUrl;

    private MultipartFile profileImageFile;

    private MultipartFile coverImageFile;
}
