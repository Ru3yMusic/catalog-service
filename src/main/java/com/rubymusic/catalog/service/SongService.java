package com.rubymusic.catalog.service;

import com.rubymusic.catalog.model.Song;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface SongService {

    Page<Song> findAll(Pageable pageable);

    /** 10 most recently added songs — Home "Escuchar ahora" + admin dashboard */
    List<Song> findRecent();

    Song findById(UUID id);

    /**
     * Returns all songs matching the provided IDs.
     * IDs with no matching song are silently skipped (returns only existing songs).
     * Used by internal batch endpoint for cross-service data enrichment.
     */
    List<Song> findByIds(List<UUID> ids);

    Page<Song> findByArtistId(UUID artistId, Pageable pageable);

    Page<Song> findByAlbumId(UUID albumId, Pageable pageable);

    Page<Song> findByGenreId(UUID genreId, Pageable pageable);

    /**
     * Songs whose genre set overlaps with any genre linked to the given station.
     * Returns an empty page if the station has no genres configured.
     */
    Page<Song> findByStationId(UUID stationId, Pageable pageable);

    Page<Song> search(String query, Pageable pageable);

    /**
     * Returns songs whose genres overlap with the genres of the given reference songs,
     * excluding those songs themselves. Used for "Canciones recomendadas" in playlist detail.
     */
    Page<Song> findRecommendations(List<UUID> referenceSongIds, Pageable pageable);

    Song create(String title, UUID artistId, UUID albumId,
                String coverUrl, String audioUrl,
                Integer duration, String lyrics, Set<UUID> genreIds);

    Song update(UUID id, String title, String coverUrl,
                String audioUrl, Integer duration, String lyrics, Set<UUID> genreIds);

    void delete(UUID id);

    /** Called by the song.played Kafka consumer */
    void incrementPlayCount(UUID songId);

    /** Called when interaction-service publishes a like event */
    void incrementLikesCount(UUID songId);

    void decrementLikesCount(UUID songId);
}
