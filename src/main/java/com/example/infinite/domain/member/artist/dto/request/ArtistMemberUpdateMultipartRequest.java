package com.example.infinite.domain.member.artist.dto.request;

import com.example.infinite.domain.member.member.enums.MemberStatus;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class ArtistMemberUpdateMultipartRequest {

    @Size(max = 100)
    private String stageName;

    @Size(max = 500)
    private String profileImageUrl;

    private MemberStatus status;

    @Positive
    private Integer sortOrder;

    private MultipartFile profileImageFile;
}
