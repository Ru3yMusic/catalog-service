package com.rubymusic.catalog.repository;

import com.rubymusic.catalog.model.Album;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AlbumRepository extends JpaRepository<Album, UUID> {

    // Override findAll to eagerly load artist — avoids LazyInitializationException
    // when mapper accesses album.getArtist() after the transaction closes
    @Override
    @EntityGraph(attributePaths = {"artist"})
    Page<Album> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"artist"})
    Page<Album> findByArtistId(UUID artistId, Pageable pageable);

    /** New releases — ordered by release date descending */
    @EntityGraph(attributePaths = {"artist"})
    Page<Album> findAllByOrderByReleaseDateDesc(Pageable pageable);

    /** Top albums — ordered by total streams descending */
    @EntityGraph(attributePaths = {"artist"})
    Page<Album> findAllByOrderByTotalStreamsDesc(Pageable pageable);

    /** Search by title (case-insensitive), newest first */
    @EntityGraph(attributePaths = {"artist"})
    Page<Album> findByTitleContainingIgnoreCaseOrderByReleaseDateDesc(String title, Pageable pageable);

    @Modifying
    @Query("UPDATE Album a SET a.totalStreams = a.totalStreams + 1 WHERE a.id = :id")
    void incrementTotalStreams(UUID id);
}
