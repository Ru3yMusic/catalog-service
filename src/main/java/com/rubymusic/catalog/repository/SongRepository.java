package com.rubymusic.catalog.repository;

import com.rubymusic.catalog.model.Song;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SongRepository extends JpaRepository<Song, UUID> {

    Page<Song> findByArtistId(UUID artistId, Pageable pageable);

    Page<Song> findByAlbumId(UUID albumId, Pageable pageable);

    /** Songs belonging to a specific genre via the song_genres join table */
    @Query("SELECT s FROM Song s JOIN s.genres g WHERE g.id = :genreId")
    Page<Song> findByGenreId(UUID genreId, Pageable pageable);

    /** Full-text search by title or artist name */
    @Query("SELECT s FROM Song s WHERE " +
           "LOWER(s.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.artist.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Song> search(String query, Pageable pageable);

    @Modifying
    @Query("UPDATE Song s SET s.playCount = s.playCount + 1 WHERE s.id = :id")
    void incrementPlayCount(UUID id);

    @Modifying
    @Query("UPDATE Song s SET s.likesCount = s.likesCount + 1 WHERE s.id = :id")
    void incrementLikesCount(UUID id);

    @Modifying
    @Query("UPDATE Song s SET s.likesCount = GREATEST(s.likesCount - 1, 0) WHERE s.id = :id")
    void decrementLikesCount(UUID id);
}
