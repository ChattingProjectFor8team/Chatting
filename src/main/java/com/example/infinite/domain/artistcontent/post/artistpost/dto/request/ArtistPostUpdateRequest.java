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
// ArtistPost 수정도 생성과 동일한 multipart 계약을 유지한다.
// 프론트는 같은 form-data 흐름으로 본문 수정과 첨부 교체를 함께 처리할 수 있다.
public class ArtistPostUpdateRequest {

    // null이면 부분 수정으로 해석해 기존 본문을 유지한다.
    @Size(max = 5000)
    private String content;

    // files 파라미터가 오면 기존 첨부 전체 교체, 없으면 기존 첨부 유지다.
    private List<MultipartFile> files;
}
