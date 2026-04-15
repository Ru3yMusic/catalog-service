package com.rubymusic.catalog.repository;

import com.rubymusic.catalog.model.Station;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StationRepository extends JpaRepository<Station, UUID> {

    /** All active stations ordered alphabetically — used for Home and onboarding picker. */
    @EntityGraph(attributePaths = {"genre"})
    List<Station> findAllByIsActiveTrueOrderByNameAsc();

    /** Case-insensitive name search — used for the search screen. */
    @EntityGraph(attributePaths = {"genre"})
    Page<Station> findByNameContainingIgnoreCaseAndIsActiveTrue(String name, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"genre"})
    Page<Station> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"genre"})
    Optional<Station> findById(UUID id);

    boolean existsByNameIgnoreCase(String name);

    /** Used by GenreServiceImpl.delete to prevent deleting a genre in use by a station */
    boolean existsByGenreId(UUID genreId);
}
