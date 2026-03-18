package com.rubymusic.catalog.service;

import com.rubymusic.catalog.model.Song;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Set;
import java.util.UUID;

public interface SongService {

    Song findById(UUID id);

    Page<Song> findByArtistId(UUID artistId, Pageable pageable);

    Page<Song> findByAlbumId(UUID albumId, Pageable pageable);

    Page<Song> findByGenreId(UUID genreId, Pageable pageable);

    Page<Song> search(String query, Pageable pageable);

    Song create(String title, UUID artistId, UUID albumId,
                String coverUrl, String audioUrl,
                Integer duration, String lyrics, Set<UUID> genreIds);

    Song update(UUID id, String title, String coverUrl,
                String audioUrl, String lyrics, Set<UUID> genreIds);

    void delete(UUID id);

    /** Called by the song.played Kafka consumer */
    void incrementPlayCount(UUID songId);

    /** Called when interaction-service publishes a like event */
    void incrementLikesCount(UUID songId);

    void decrementLikesCount(UUID songId);
}
