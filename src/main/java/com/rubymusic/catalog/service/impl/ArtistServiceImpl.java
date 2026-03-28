package com.rubymusic.catalog.service.impl;

import com.rubymusic.catalog.model.Artist;
import com.rubymusic.catalog.repository.ArtistRepository;
import com.rubymusic.catalog.repository.SongRepository;
import com.rubymusic.catalog.service.ArtistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtistServiceImpl implements ArtistService {

    private final ArtistRepository artistRepository;
    private final SongRepository songRepository;

    @Override
    public Page<Artist> findAll(Boolean isTop, Pageable pageable) {
        if (Boolean.TRUE.equals(isTop)) {
            return artistRepository.findByIsTopTrue(pageable);
        }
        return artistRepository.findAll(pageable);
    }

    @Override
    public Artist findById(UUID id) {
        return artistRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Artist not found: " + id));
    }

    @Override
    public List<Artist> findTopArtists() {
        return artistRepository.findByIsTopTrueOrderByMonthlyListenersDesc();
    }

    @Override
    public Page<Artist> search(String query, Pageable pageable) {
        return artistRepository.findByNameContainingIgnoreCase(query, pageable);
    }

    @Override
    @Transactional
    public Artist create(String name, String photoUrl, String bio, Boolean isTop) {
        Artist artist = Artist.builder()
                .name(name)
                .photoUrl(photoUrl)
                .bio(bio)
                .isTop(Boolean.TRUE.equals(isTop))
                .build();
        return artistRepository.save(artist);
    }

    @Override
    @Transactional
    public Artist update(UUID id, String name, String photoUrl, String bio, Boolean isTop) {
        Artist artist = findById(id);
        if (name != null && !name.isBlank()) artist.setName(name);
        if (photoUrl != null) artist.setPhotoUrl(photoUrl);
        if (bio != null) artist.setBio(bio);
        if (isTop != null) artist.setIsTop(isTop);
        return artistRepository.save(artist);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        songRepository.removeArtistSongsFromAllStations(id);
        artistRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void incrementFollowersCount(UUID artistId) {
        artistRepository.incrementFollowersCount(artistId);
    }

    @Override
    @Transactional
    public void decrementFollowersCount(UUID artistId) {
        artistRepository.decrementFollowersCount(artistId);
    }
}
