package com.example.infinite.domain.artistcontent.media.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

class YoutubeUrlParserTest {

    @Test
    void watchUrl에서VideoId를추출한다() {
        assertThat(YoutubeUrlParser.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
                .isEqualTo("dQw4w9WgXcQ");
    }

    @Test
    void shortsUrl에서도VideoId를추출한다() {
        assertThat(YoutubeUrlParser.extractVideoId("https://youtube.com/shorts/dQw4w9WgXcQ"))
                .isEqualTo("dQw4w9WgXcQ");
    }

    @Test
    void 스킴없는WatchUrl에서도VideoId를추출한다() {
        assertThat(YoutubeUrlParser.extractVideoId("www.youtube.com/watch?v=dQw4w9WgXcQ"))
                .isEqualTo("dQw4w9WgXcQ");
    }

    @Test
    void 스킴없는ShortUrl에서도VideoId를추출한다() {
        assertThat(YoutubeUrlParser.extractVideoId("youtu.be/dQw4w9WgXcQ"))
                .isEqualTo("dQw4w9WgXcQ");
    }

    @Test
    void youtube로끝나는가짜호스트는거부한다() {
        assertThatThrownBy(() -> YoutubeUrlParser.extractVideoId("https://notyoutube.com/watch?v=dQw4w9WgXcQ"))
                .isInstanceOf(RuntimeException.class);
    }
}
