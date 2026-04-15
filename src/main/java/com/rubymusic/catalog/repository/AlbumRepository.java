package com.rubymusic.catalog.repository;

import com.rubymusic.catalog.model.Album;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlbumRepository extends JpaRepository<Album, UUID> {

    // Every read method that returns Album(s) eagerly loads 'artist' AND 'station' so
    // AlbumMapper can call album.getArtist().getName() and album.getStation().getName()
    // safely after the transaction closes (controller layer).  Without these graphs the
    // LAZY proxies are uninitialized by the time MapStruct runs, causing
    // LazyInitializationException.

    /** Single-album lookup — used by getAlbumById, updateAlbum, createAlbum result. */
    @Override
    @EntityGraph(attributePaths = {"artist", "station"})
    Optional<Album> findById(UUID id);

    /** Paginated list — all albums or filtered by artist. */
    @Override
    @EntityGraph(attributePaths = {"artist", "station"})
    Page<Album> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"artist", "station"})
    Page<Album> findByArtistId(UUID artistId, Pageable pageable);

    /** New releases — ordered by release date descending */
    @EntityGraph(attributePaths = {"artist", "station"})
    Page<Album> findAllByOrderByReleaseDateDesc(Pageable pageable);

    /** Top albums — ordered by total streams descending */
    @EntityGraph(attributePaths = {"artist", "station"})
    Page<Album> findAllByOrderByTotalStreamsDesc(Pageable pageable);

    /** Search by title (case-insensitive), newest first */
    @EntityGraph(attributePaths = {"artist", "station"})
    Page<Album> findByTitleContainingIgnoreCaseOrderByReleaseDateDesc(String title, Pageable pageable);

    @Modifying
    @Query("UPDATE Album a SET a.totalStreams = a.totalStreams + 1 WHERE a.id = :id")
    void incrementTotalStreams(UUID id);
}
