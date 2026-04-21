package com.example.infinite.domain.member.artist.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class ArtistMemberCreateMultipartRequest {

    @NotNull
    @Positive
    private Long memberId;

    @NotBlank
    @Size(max = 100)
    private String stageName;

    @Size(max = 500)
    private String profileImageUrl;

    @NotNull
    @Positive
    private Integer sortOrder;

    private MultipartFile profileImageFile;
}
