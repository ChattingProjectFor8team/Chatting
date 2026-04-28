package com.example.infinite.domain.artistcontent.post.fanpost.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class FanPostUpdateRequest {

    // 수정은 부분 변경이므로 null이면 기존 본문 유지로 해석한다.
    @Size(max = 5000)
    private String content;

    // files 파라미터를 보내면 기존 미디어 전체를 교체하고, 아예 보내지 않으면 기존 미디어를 유지한다.
    private List<MultipartFile> files;
}
