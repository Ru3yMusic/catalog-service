package com.rubymusic.catalog.service;

import com.rubymusic.catalog.model.Album;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public interface AlbumService {

    /** @param artistId when non-null, filters by artist */
    Page<Album> findAll(UUID artistId, Pageable pageable);

    Album findById(UUID id);

    Page<Album> findByArtistId(UUID artistId, Pageable pageable);

    Page<Album> findNewReleases(Pageable pageable);

    Page<Album> findTopByStreams(Pageable pageable);

    Page<Album> search(String query, Pageable pageable);

    Album create(String title, UUID artistId, String coverUrl, LocalDate releaseDate, UUID stationId);

    Album update(UUID id, String title, String coverUrl, LocalDate releaseDate, UUID stationId);

    void delete(UUID id);
}
