package com.rubymusic.catalog.service.impl;

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

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlbumServiceImpl implements AlbumService {

    private final AlbumRepository albumRepository;
    private final ArtistRepository artistRepository;
    private final StationRepository stationRepository;
    private final SongRepository songRepository;

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
        return albumRepository.findAllByOrderByReleaseDateDesc(pageable);
    }

    @Override
    public Page<Album> findTopByStreams(Pageable pageable) {
        return albumRepository.findAllByOrderByTotalStreamsDesc(pageable);
    }

    @Override
    public Page<Album> search(String query, Pageable pageable) {
        return albumRepository.findByTitleContainingIgnoreCaseOrderByReleaseDateDesc(query, pageable);
    }

    @Override
    @Transactional
    public Album create(String title, UUID artistId, String coverUrl, LocalDate releaseDate, UUID stationId) {
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
                .releaseDate(releaseDate)
                .station(station)
                .build();
        return albumRepository.save(album);
    }

    @Override
    @Transactional
    public Album update(UUID id, String title, String coverUrl, LocalDate releaseDate, UUID stationId) {
        Album album = findById(id);
        if (title != null && !title.isBlank()) album.setTitle(title);
        if (coverUrl != null) album.setCoverUrl(coverUrl);
        if (releaseDate != null) album.setReleaseDate(releaseDate);
        if (stationId != null) {
            Station station = stationRepository.findById(stationId)
                    .orElseThrow(() -> new IllegalArgumentException("Station not found: " + stationId));
            album.setStation(station);
        }
        return albumRepository.save(album);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        songRepository.removeAlbumSongsFromAllStations(id);
        albumRepository.deleteById(id);
    }
}
