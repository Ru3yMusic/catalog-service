package com.rubymusic.catalog.service.impl;

import com.rubymusic.catalog.event.MusicFeedEventPublisher;
import com.rubymusic.catalog.model.Album;
import com.rubymusic.catalog.model.Artist;
import com.rubymusic.catalog.model.Station;
import com.rubymusic.catalog.repository.AlbumRepository;
import com.rubymusic.catalog.repository.ArtistRepository;
import com.rubymusic.catalog.repository.SongRepository;
import com.rubymusic.catalog.repository.StationRepository;
import com.rubymusic.catalog.service.AlbumService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlbumServiceImpl implements AlbumService {

    private final AlbumRepository albumRepository;
    private final ArtistRepository artistRepository;
    private final StationRepository stationRepository;
    private final SongRepository songRepository;
    private final MusicFeedEventPublisher musicFeedEventPublisher;

    @Override
    public Page<Album> findAll(UUID artistId, Pageable pageable) {
        if (artistId != null) {
            return albumRepository.findByArtistId(artistId, pageable);
        }
        return albumRepository.findAll(pageable);
    }

    @Override
    public Album findById(UUID id) {
        return albumRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Album not found: " + id));
    }

    @Override
    public Page<Album> findByArtistId(UUID artistId, Pageable pageable) {
        return albumRepository.findByArtistId(artistId, pageable);
    }

    @Override
    public Page<Album> findNewReleases(Pageable pageable) {
        return albumRepository.findAllByReleasedTrueOrderByReleaseDateTimeDesc(pageable);
    }

    @Override
    public Page<Album> findTopByStreams(Pageable pageable) {
        return albumRepository.findAllByOrderByTotalStreamsDesc(pageable);
    }

    @Override
    public Page<Album> search(String query, Pageable pageable) {
        return albumRepository.findByTitleContainingIgnoreCaseOrderByReleaseDateTimeDesc(query, pageable);
    }

    @Override
    @Transactional
    public Album create(String title, UUID artistId, String coverUrl, LocalDateTime releaseDateTime, UUID stationId) {
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new IllegalArgumentException("Artist not found: " + artistId));
        Station station = stationId != null
                ? stationRepository.findById(stationId)
                        .orElseThrow(() -> new IllegalArgumentException("Station not found: " + stationId))
                : null;
        Album album = Album.builder()
                .title(title)
                .artist(artist)
                .coverUrl(coverUrl)
                .releaseDateTime(releaseDateTime)
                .released(!releaseDateTime.isAfter(LocalDateTime.now()))
                .station(station)
                .build();
        Album saved = albumRepository.save(album);

        // Emit only when the album is publicly visible RIGHT NOW. Future-scheduled
        // albums (released=false) will fire later via AlbumReleaseScheduler.
        if (Boolean.TRUE.equals(saved.getReleased())) {
            musicFeedEventPublisher.publishAlbumReleased(saved);
        }
        return saved;
    }

    @Override
    @Transactional
    public Album update(UUID id, String title, String coverUrl, LocalDateTime releaseDateTime, UUID stationId) {
        Album album = findById(id);
        boolean wasReleased = Boolean.TRUE.equals(album.getReleased());

        if (title != null && !title.isBlank()) album.setTitle(title);
        if (coverUrl != null) album.setCoverUrl(coverUrl);
        if (releaseDateTime != null) {
            album.setReleaseDateTime(releaseDateTime);
            // Re-evaluate `released`: if the admin pushes the date into the future, the album
            // hides again; if they pull it back to the past, it becomes visible immediately.
            album.setReleased(!releaseDateTime.isAfter(LocalDateTime.now()));
        }
        if (stationId != null) {
            Station station = stationRepository.findById(stationId)
                    .orElseThrow(() -> new IllegalArgumentException("Station not found: " + stationId));
            album.setStation(station);
        }
        Album saved = albumRepository.save(album);

        // Emit ONLY on the false → true transition (admin pulled date back into the past).
        // Future-pushed dates (true → false) don't broadcast — clients still see the card
        // until next refresh, then it's filtered out by the released=true repo query.
        if (!wasReleased && Boolean.TRUE.equals(saved.getReleased())) {
            musicFeedEventPublisher.publishAlbumReleased(saved);
        }
        return saved;
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        songRepository.removeAlbumSongsFromAllStations(id);
        albumRepository.deleteById(id);
    }
}
