package com.example.infinite.domain.member.artist.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class ArtistUpdateMultipartRequest {

    @Size(max = 100)
    private String name;

    @Pattern(regexp = "^[A-Za-z0-9-]+$")
    @Size(max = 100)
    private String slug;

    @Size(max = 500)
    private String profileImageUrl;

    @Size(max = 500)
    private String coverImageUrl;

    @Size(max = 5000)
    private String intro;

    private MultipartFile profileImageFile;

    private MultipartFile coverImageFile;
}
