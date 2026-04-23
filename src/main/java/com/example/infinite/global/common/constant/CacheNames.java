package com.example.infinite.global.common.constant;

// Spring Cache 이름을 문자열 리터럴 대신 상수로 모아 관리한다.
public final class CacheNames {

    public static final String ARTIST_DETAIL_V2 = "artistDetailV2";
    public static final String ARTIST_POST_LIST_BASE = "artistPostListBase";
    public static final String ARTIST_POST_DETAIL_BASE = "artistPostDetailBase";
    public static final String FAN_POST_LIST_BASE = "fanPostListBase";
    public static final String FAN_POST_DETAIL_BASE = "fanPostDetailBase";
    public static final String FAN_LETTER_LIST_BASE = "fanLetterListBase";
    public static final String FAN_LETTER_DETAIL_BASE = "fanLetterDetailBase";
    public static final String POST_HOT_DATA = "postHotData";
    public static final String COMMENT_ROOT_SLICE = "commentRootSlice";
    public static final String COMMENT_REPLY_LIST = "commentReplyList";

    private CacheNames() {
    }
}
