package com.rubymusic.catalog.repository;

import com.rubymusic.catalog.model.Song;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface SongRepository extends JpaRepository<Song, UUID> {

    // JpaRepository.findAll(Pageable) and findById(UUID) have no @EntityGraph by default.
    // Override them so that SongMapper can access artist, album, album.station, and genres
    // safely even when spring.jpa.open-in-view is false.
    @Override
    @EntityGraph(attributePaths = {"artist", "album", "album.station", "genres"})
    Page<Song> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"artist", "album", "album.station", "genres"})
    Optional<Song> findById(UUID id);

    // Song.artist, Song.album, and Song.genres are all LAZY.  SongMapper accesses all
    // three via ArtistMapper, AlbumMapper and GenreMapper.  The mapping runs in the
    // controller layer after the transaction has already closed, so every method that
    // returns Song data consumed by the mapper must eagerly fetch these associations.
    // genres is a Set (not a List/bag) so fetching it alongside the two @ManyToOne
    // associations does NOT trigger Hibernate's "cannot simultaneously fetch multiple bags"
    // error.

    @EntityGraph(attributePaths = {"artist", "album", "album.station", "genres"})
    Page<Song> findByArtistId(UUID artistId, Pageable pageable);

    @EntityGraph(attributePaths = {"artist", "album", "album.station", "genres"})
    Page<Song> findByAlbumId(UUID albumId, Pageable pageable);

    /** Songs belonging to a specific genre via the song_genres join table */
    @EntityGraph(attributePaths = {"artist", "album", "album.station", "genres"})
    @Query("SELECT s FROM Song s JOIN s.genres g WHERE g.id = :genreId")
    Page<Song> findByGenreId(UUID genreId, Pageable pageable);

    /**
     * Songs whose genre set overlaps with the provided genre IDs.
     * Used for genre-based queries (e.g. genre songs list, recommendations).
     */
    @EntityGraph(attributePaths = {"artist", "album", "album.station", "genres"})
    @Query("SELECT DISTINCT s FROM Song s JOIN s.genres g WHERE g.id IN :genreIds")
    Page<Song> findByGenreIds(Set<UUID> genreIds, Pageable pageable);

    /**
     * Songs directly assigned to a station via the station_songs join table.
     * Traverses Station.songs (owning side of ManyToMany).
     */
    @EntityGraph(attributePaths = {"artist", "album", "album.station", "genres"})
    @Query("SELECT s FROM Station st JOIN st.songs s WHERE st.id = :stationId")
    Page<Song> findByStationId(@Param("stationId") UUID stationId, Pageable pageable);

    /** Full-text search by title or artist name.
     *  The sentinel ":query = ''" short-circuits the LOWER() calls when the
     *  caller passes an empty string, avoiding the lower(bytea) error that
     *  PostgreSQL raises when Hibernate cannot infer a concrete type for a
     *  NULL-or-untyped parameter.  The service layer guarantees non-null input. */
    @EntityGraph(attributePaths = {"artist", "album", "album.station", "genres"})
    @Query("SELECT s FROM Song s WHERE " +
           ":query = '' OR " +
           "LOWER(s.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.artist.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Song> search(@Param("query") String query, Pageable pageable);

    /**
     * Collects all genre IDs linked to the given song IDs.
     * Used as input for the recommendations query.
     */
    @Query("SELECT DISTINCT g.id FROM Song s JOIN s.genres g WHERE s.id IN :songIds")
    List<UUID> findGenreIdsBySongIds(@Param("songIds") List<UUID> songIds);

    /**
     * Songs whose genres overlap with the provided genre IDs, excluding the reference songs.
     * Used for "Canciones recomendadas" in playlist detail.
     */
    @EntityGraph(attributePaths = {"artist", "album", "album.station", "genres"})
    @Query("SELECT DISTINCT s FROM Song s JOIN s.genres g WHERE g.id IN :genreIds AND s.id NOT IN :excludeIds")
    Page<Song> findRecommendations(@Param("genreIds") List<UUID> genreIds,
                                   @Param("excludeIds") List<UUID> excludeIds,
                                   Pageable pageable);

    /**
     * Removes a song from all station_songs associations before the Song entity is deleted.
     * Required because Station owns the ManyToMany — JPA won't clean up this side automatically.
     */
    @Modifying
    @Query(value = "DELETE FROM station_songs WHERE song_id = :songId", nativeQuery = true)
    void removeFromAllStations(@Param("songId") UUID songId);

    /**
     * Removes all songs of an album from station_songs before album (and its songs) are deleted.
     */
    @Modifying
    @Query(value = "DELETE FROM station_songs WHERE song_id IN (SELECT id FROM songs WHERE album_id = :albumId)", nativeQuery = true)
    void removeAlbumSongsFromAllStations(@Param("albumId") UUID albumId);

    /**
     * Removes all songs of an artist from station_songs before artist (and its songs) are deleted.
     */
    @Modifying
    @Query(value = "DELETE FROM station_songs WHERE song_id IN (SELECT id FROM songs WHERE artist_id = :artistId)", nativeQuery = true)
    void removeArtistSongsFromAllStations(@Param("artistId") UUID artistId);

    @Modifying
    @Query("UPDATE Song s SET s.playCount = s.playCount + 1 WHERE s.id = :id")
    void incrementPlayCount(UUID id);

    @Modifying
    @Query("UPDATE Song s SET s.likesCount = s.likesCount + 1 WHERE s.id = :id")
    void incrementLikesCount(UUID id);

    @Modifying
    @Query("UPDATE Song s SET s.likesCount = GREATEST(s.likesCount - 1, 0) WHERE s.id = :id")
    void decrementLikesCount(UUID id);

    /** 10 most recently added songs — used by Home "Escuchar ahora" and admin dashboard */
    @EntityGraph(attributePaths = {"artist", "album", "album.station", "genres"})
    List<Song> findTop10ByOrderByCreatedAtDesc();

    /**
     * Returns the artist ID for a given song without loading the full Song entity.
     * Used by {@code incrementPlayCount} to update artist monthly listeners atomically.
     */
    @Query("SELECT s.artist.id FROM Song s WHERE s.id = :id")
    Optional<UUID> findArtistIdBySongId(@Param("id") UUID id);

    /**
     * Returns the album ID for a given song without loading the full Song entity.
     * Used by {@code incrementPlayCount} to update album total streams.
     * Returns empty when the song is a single (no album).
     */
    @Query("SELECT s.album.id FROM Song s WHERE s.id = :id")
    Optional<UUID> findAlbumIdBySongId(@Param("id") UUID id);
}
