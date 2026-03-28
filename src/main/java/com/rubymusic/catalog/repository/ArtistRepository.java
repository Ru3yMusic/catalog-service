package com.rubymusic.catalog.repository;

import com.rubymusic.catalog.model.Artist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ArtistRepository extends JpaRepository<Artist, UUID> {

    List<Artist> findByIsTopTrueOrderByMonthlyListenersDesc();

    Page<Artist> findByIsTopTrue(Pageable pageable);

    Page<Artist> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Modifying
    @Query("UPDATE Artist a SET a.followersCount = a.followersCount + 1 WHERE a.id = :id")
    void incrementFollowersCount(UUID id);

    @Modifying
    @Query("UPDATE Artist a SET a.followersCount = GREATEST(a.followersCount - 1, 0) WHERE a.id = :id")
    void decrementFollowersCount(UUID id);

    @Modifying
    @Query("UPDATE Artist a SET a.monthlyListeners = a.monthlyListeners + 1 WHERE a.id = :id")
    void incrementMonthlyListeners(UUID id);
}
