package com.example.infinite.domain.artistcontent.post.fanletter.enums;

// 팬레터 수신 대상은 그룹(artist) 자체이거나, 특정 artist-member 둘 중 하나다.
public enum FanLetterRecipientType {
    // To.세븐틴 같이 아티스트 전체에게 보내는 팬레터
    ARTIST,
    // To.민규 같이 특정 artist-member 에게 보내는 팬레터
    ARTIST_MEMBER
}
