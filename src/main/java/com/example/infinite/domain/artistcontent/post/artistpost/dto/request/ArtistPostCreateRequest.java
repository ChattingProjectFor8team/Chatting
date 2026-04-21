package com.example.infinite.domain.artistcontent.post.artistpost.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ArtistPostCreateRequest {

    // FanPost와 같은 multipart 패턴을 재사용해 본문과 첨부를 함께 바인딩한다.
    @Size(max = 5000)
    private String content;

    // 공식 게시글도 이미지 여러 장 또는 동영상 1개 정책은 동일하다.
    private List<MultipartFile> files;
}
