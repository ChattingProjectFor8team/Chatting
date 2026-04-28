package com.example.infinite.domain.artistcontent.post.fanletter.dto.request;

import com.example.infinite.domain.artistcontent.post.fanletter.enums.FanLetterRecipientType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
// multipart/form-data 로 받는 팬레터 생성 요청이다.
// 텍스트 본문 대신 "수신 대상 + 이미지 한 장"만 받는다.
public class FanLetterCreateRequest {

    // 그룹 전체에게 보낼지, 특정 artist-member 에게 보낼지 선택한다.
    @NotNull
    private FanLetterRecipientType recipientType;

    // recipientType=ARTIST_MEMBER 일 때만 필요하다.
    private Long recipientArtistMemberId;

    // 팬레터는 반드시 이미지 한 장만 업로드한다.
    private MultipartFile image;
}
