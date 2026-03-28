package com.rubymusic.catalog.service.impl;

import com.rubymusic.catalog.model.Genre;
import com.rubymusic.catalog.model.Song;
import com.rubymusic.catalog.model.Station;
import com.rubymusic.catalog.repository.GenreRepository;
import com.rubymusic.catalog.repository.SongRepository;
import com.rubymusic.catalog.repository.StationRepository;
import com.rubymusic.catalog.service.StationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StationServiceImpl implements StationService {

    private final StationRepository stationRepository;
    private final GenreRepository genreRepository;
    private final SongRepository songRepository;

    @Override
    public List<Station> findAllActive() {
        return stationRepository.findAllByIsActiveTrueOrderByNameAsc();
    }

    @Override
    public Page<Station> findAll(Pageable pageable) {
        return stationRepository.findAll(pageable);
    }

    @Override
    public Station findById(UUID id) {
        return stationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Station not found: " + id));
    }

    @Override
    public Page<Station> search(String query, Pageable pageable) {
        return stationRepository.findByNameContainingIgnoreCaseAndIsActiveTrue(query, pageable);
    }

    @Override
    @Transactional
    public Station create(String name, UUID genreId, String gradientStart, String gradientEnd, Set<UUID> songIds) {
        if (stationRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Station already exists: " + name);
        }
        Genre genre = genreRepository.findById(genreId)
                .orElseThrow(() -> new IllegalArgumentException("Genre not found: " + genreId));
        Set<Song> songs = resolveSongs(songIds);
        if (songs.size() < 3) {
            throw new IllegalArgumentException("A station requires at least 3 songs");
        }
        Station station = Station.builder()
                .name(name)
                .genre(genre)
                .gradientStart(gradientStart)
                .gradientEnd(gradientEnd)
                .songs(songs)
                .build();
        return stationRepository.save(station);
    }

    @Override
    @Transactional
    public Station update(UUID id, String name, UUID genreId, String gradientStart,
                          String gradientEnd, Boolean isActive, Set<UUID> songIds) {
        Station station = findById(id);
        if (name != null && !name.isBlank()) station.setName(name);
        if (gradientStart != null) station.setGradientStart(gradientStart);
        if (gradientEnd != null) station.setGradientEnd(gradientEnd);
        if (isActive != null) station.setIsActive(isActive);
        if (genreId != null) {
            Genre genre = genreRepository.findById(genreId)
                    .orElseThrow(() -> new IllegalArgumentException("Genre not found: " + genreId));
            station.setGenre(genre);
        }
        if (songIds != null) {
            Set<Song> songs = resolveSongs(songIds);
            if (songs.size() < 3) {
                throw new IllegalArgumentException("A station requires at least 3 songs");
            }
            station.setSongs(songs);
        }
        return stationRepository.save(station);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        stationRepository.deleteById(id);
    }

    private Set<Song> resolveSongs(Set<UUID> songIds) {
        if (songIds == null || songIds.isEmpty()) return new HashSet<>();
        Set<Song> songs = new HashSet<>(songRepository.findAllById(songIds));
        if (songs.size() != songIds.size()) {
            throw new IllegalArgumentException("One or more song IDs are invalid");
        }
        return songs;
    }
}
