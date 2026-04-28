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
// ArtistPost 생성은 FanPost와 같은 multipart 계약을 사용한다.
// 본문과 첨부를 분리 API로 나누지 않고 한 번에 받아 공식 게시글 작성 UX를 단순하게 유지한다.
public class ArtistPostCreateRequest {

    // FanPost와 같은 multipart 패턴을 재사용해 본문과 첨부를 함께 바인딩한다.
    @Size(max = 5000)
    private String content;

    // 공식 게시글도 이미지 여러 장 또는 동영상 1개 정책은 동일하다.
    private List<MultipartFile> files;
}
