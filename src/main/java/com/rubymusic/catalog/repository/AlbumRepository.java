package com.rubymusic.catalog.repository;

import com.rubymusic.catalog.model.Album;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AlbumRepository extends JpaRepository<Album, UUID> {

    Page<Album> findByArtistId(UUID artistId, Pageable pageable);

    /** New releases — ordered by release date descending */
    Page<Album> findAllByOrderByReleaseDateDesc(Pageable pageable);

    /** Top albums — ordered by total streams descending */
    Page<Album> findAllByOrderByTotalStreamsDesc(Pageable pageable);

    /** Search by title (case-insensitive), newest first */
    Page<Album> findByTitleContainingIgnoreCaseOrderByReleaseDateDesc(String title, Pageable pageable);

    @Modifying
    @Query("UPDATE Album a SET a.totalStreams = a.totalStreams + 1 WHERE a.id = :id")
    void incrementTotalStreams(UUID id);
}
