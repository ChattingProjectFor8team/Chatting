package com.example.infinite.domain.member.artist.repository;

import com.example.infinite.domain.member.artist.dto.response.ArtistDetailRow;
import com.example.infinite.domain.member.artist.dto.response.ArtistSearchResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ArtistRepositoryCustom {

    Page<ArtistSearchResponse> searchArtists(String keyword, int page, int size);

    List<ArtistDetailRow> findArtistDetailRows(Long artistId);
}
