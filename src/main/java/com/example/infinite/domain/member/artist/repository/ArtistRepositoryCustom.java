package com.example.infinite.domain.member.artist.repository;

import com.example.infinite.domain.member.artist.dto.response.ArtistSearchResponse;
import org.springframework.data.domain.Page;

public interface ArtistRepositoryCustom {

    Page<ArtistSearchResponse> searchArtists(String keyword, int size);
}
