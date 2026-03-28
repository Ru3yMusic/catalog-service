package com.rubymusic.catalog.service;

import com.rubymusic.catalog.model.Artist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ArtistService {

    /** @param isTop when true, returns only top artists; null = all */
    Page<Artist> findAll(Boolean isTop, Pageable pageable);

    Artist findById(UUID id);

    List<Artist> findTopArtists();

    Page<Artist> search(String query, Pageable pageable);

    Artist create(String name, String photoUrl, String bio, Boolean isTop);

    Artist update(UUID id, String name, String photoUrl, String bio, Boolean isTop);

    void delete(UUID id);

    void incrementFollowersCount(UUID artistId);

    void decrementFollowersCount(UUID artistId);
}
