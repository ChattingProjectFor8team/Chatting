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
public class ArtistPostUpdateRequest {

    // null이면 부분 수정으로 해석해 기존 본문을 유지한다.
    @Size(max = 5000)
    private String content;

    // files 파라미터가 오면 기존 첨부 전체 교체, 없으면 기존 첨부 유지다.
    private List<MultipartFile> files;
}
