package com.rubymusic.catalog.service;

import com.rubymusic.catalog.model.Station;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;
import java.util.UUID;


public interface StationService {

    /**
     * Returns all active stations ordered by name.
     * Used in: Home screen station chips, onboarding preference picker.
     */
    List<Station> findAllActive();

    /** Paginated list of all stations (admin view, includes inactive). */
    Page<Station> findAll(Pageable pageable);

    /** Throws IllegalArgumentException if not found. */
    Station findById(UUID id);

    /** Case-insensitive name search among active stations only. */
    Page<Station> search(String query, Pageable pageable);

    /**
     * Creates a new station.
     * @param genreId ID of the single genre that categorizes this station.
     * @param songIds IDs of songs directly assigned (minimum 3 enforced).
     * @throws IllegalArgumentException if name already exists, genreId is invalid, or fewer than 3 songs.
     */
    Station create(String name, UUID genreId, String gradientStart, String gradientEnd, Set<UUID> songIds);

    /**
     * Partial update — null values are ignored.
     * @param songIds when non-null, replaces the full song set.
     */
    Station update(UUID id, String name, UUID genreId, String gradientStart,
                   String gradientEnd, Boolean isActive, Set<UUID> songIds);

    void delete(UUID id);
}
