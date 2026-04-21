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
public class FanPostCreateRequest {

    // multipart/form-data 로 본문과 첨부파일을 함께 받기 위해 record 대신 model attribute 바인딩 가능한 클래스로 둔다.
    @Size(max = 5000)
    private String content;

    // 팬포스트는 이미지 여러 장 또는 동영상 1개를 본문과 함께 받는다.
    private List<MultipartFile> files;
}
