package com.rubymusic.catalog.service.impl;

import com.rubymusic.catalog.model.Album;
import com.rubymusic.catalog.model.Artist;
import com.rubymusic.catalog.model.Genre;
import com.rubymusic.catalog.model.Song;
import com.rubymusic.catalog.repository.AlbumRepository;
import com.rubymusic.catalog.repository.ArtistRepository;
import com.rubymusic.catalog.repository.GenreRepository;
import com.rubymusic.catalog.repository.SongRepository;
import com.rubymusic.catalog.service.SongService;
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
public class SongServiceImpl implements SongService {

    private final SongRepository songRepository;
    private final ArtistRepository artistRepository;
    private final AlbumRepository albumRepository;
    private final GenreRepository genreRepository;

    @Override
    public Page<Song> findAll(Pageable pageable) {
        return songRepository.findAll(pageable);
    }

    @Override
    public List<Song> findRecent() {
        return songRepository.findTop10ByOrderByCreatedAtDesc();
    }

    @Override
    public Song findById(UUID id) {
        return songRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Song not found: " + id));
    }

    @Override
    public List<Song> findByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return songRepository.findAllById(ids);
    }

    @Override
    public Page<Song> findByArtistId(UUID artistId, Pageable pageable) {
        return songRepository.findByArtistId(artistId, pageable);
    }

    @Override
    public Page<Song> findByAlbumId(UUID albumId, Pageable pageable) {
        return songRepository.findByAlbumId(albumId, pageable);
    }

    @Override
    public Page<Song> findByGenreId(UUID genreId, Pageable pageable) {
        return songRepository.findByGenreId(genreId, pageable);
    }

    @Override
    public Page<Song> findByStationId(UUID stationId, Pageable pageable) {
        return songRepository.findByStationId(stationId, pageable);
    }

    @Override
    public Page<Song> search(String query, Pageable pageable) {
        // Sanitize: null collapses to empty string so the repository sentinel
        // ":query = ''" triggers and returns all songs instead of blowing up
        // with a lower(bytea) error in PostgreSQL.
        if (query == null) query = "";
        return songRepository.search(query, pageable);
    }

    @Override
    public Page<Song> findRecommendations(List<UUID> referenceSongIds, Pageable pageable) {
        if (referenceSongIds == null || referenceSongIds.isEmpty()) {
            return org.springframework.data.domain.Page.empty(pageable);
        }
        List<UUID> genreIds = songRepository.findGenreIdsBySongIds(referenceSongIds);
        if (genreIds.isEmpty()) {
            return org.springframework.data.domain.Page.empty(pageable);
        }
        return songRepository.findRecommendations(genreIds, referenceSongIds, pageable);
    }

    @Override
    @Transactional
    public Song create(String title, UUID artistId, UUID albumId,
                       String coverUrl, String audioUrl,
                       Integer duration, String lyrics, Set<UUID> genreIds) {
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new IllegalArgumentException("Artist not found: " + artistId));

        Album album = null;
        if (albumId != null) {
            album = albumRepository.findById(albumId)
                    .orElseThrow(() -> new IllegalArgumentException("Album not found: " + albumId));
        }

        Set<Genre> genres = resolveGenres(genreIds);

        Song song = Song.builder()
                .title(title)
                .artist(artist)
                .album(album)
                .coverUrl(coverUrl)
                .audioUrl(audioUrl)
                .duration(duration)
                .lyrics(lyrics)
                .genres(genres)
                .build();

        return songRepository.save(song);
    }

    @Override
    @Transactional
    public Song update(UUID id, String title, String coverUrl,
                       String audioUrl, Integer duration, String lyrics, Set<UUID> genreIds) {
        Song song = findById(id);
        if (title != null && !title.isBlank()) song.setTitle(title);
        if (coverUrl != null) song.setCoverUrl(coverUrl);
        if (audioUrl != null) song.setAudioUrl(audioUrl);
        if (duration != null) song.setDuration(duration);
        if (lyrics != null) song.setLyrics(lyrics);
        if (genreIds != null) song.setGenres(resolveGenres(genreIds));
        return songRepository.save(song);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        songRepository.removeFromAllStations(id);
        songRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void incrementPlayCount(UUID songId) {
        songRepository.incrementPlayCount(songId);
        // Also keep the artist's monthlyListeners counter in sync — same transaction
        songRepository.findArtistIdBySongId(songId)
                .ifPresent(artistRepository::incrementMonthlyListeners);
    }

    @Override
    @Transactional
    public void incrementLikesCount(UUID songId) {
        songRepository.incrementLikesCount(songId);
    }

    @Override
    @Transactional
    public void decrementLikesCount(UUID songId) {
        songRepository.decrementLikesCount(songId);
    }

    private Set<Genre> resolveGenres(Set<UUID> genreIds) {
        if (genreIds == null || genreIds.isEmpty()) return new HashSet<>();
        Set<Genre> genres = new HashSet<>(genreRepository.findAllById(genreIds));
        if (genres.size() != genreIds.size()) {
            throw new IllegalArgumentException("One or more genre IDs are invalid");
        }
        return genres;
    }
}
