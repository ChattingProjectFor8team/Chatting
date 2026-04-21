package com.example.infinite.domain.artistcontent.post.fanletter.dto.request;

import com.example.infinite.domain.artistcontent.post.fanletter.enums.FanLetterRecipientType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
// 팬레터 수정은 부분 변경 방식이다.
// null 로 들어온 값은 기존 상태를 유지하고, image 가 오면 기존 이미지를 통째로 교체한다.
public class FanLetterUpdateRequest {

    // 수정은 부분 변경이므로 null 이면 기존 수신 대상을 유지한다.
    private FanLetterRecipientType recipientType;

    // recipientType=ARTIST_MEMBER 로 바꿀 때 대상 artist-member 를 함께 보낸다.
    private Long recipientArtistMemberId;

    // 이미지를 보내면 기존 팬레터 이미지를 전체 교체한다.
    private MultipartFile image;
}
