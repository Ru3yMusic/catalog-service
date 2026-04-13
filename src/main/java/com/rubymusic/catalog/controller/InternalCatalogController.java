package com.rubymusic.catalog.controller;

import com.rubymusic.catalog.dto.ArtistInternalDto;
import com.rubymusic.catalog.dto.SongInternalDto;
import com.rubymusic.catalog.model.Artist;
import com.rubymusic.catalog.model.Song;
import com.rubymusic.catalog.service.ArtistService;
import com.rubymusic.catalog.service.SongService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Internal catalog endpoints — accessible only with ROLE_SERVICE JWT.
 *
 * <p>Implements the generated {@link InternalApi} interface from the OpenAPI spec.
 * Security is enforced by {@link com.rubymusic.catalog.config.SecurityConfig}:
 * {@code /api/internal/v1/**} requires {@code ROLE_SERVICE}.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class InternalCatalogController implements InternalApi {

    private final SongService songService;
    private final ArtistService artistService;

    @Override
    public ResponseEntity<SongInternalDto> getInternalSongById(UUID id) {
        log.debug("Internal: getSong id={}", id);
        Song song = songService.findById(id);
        return ResponseEntity.ok(toSongDto(song));
    }

    @Override
    public ResponseEntity<List<SongInternalDto>> getInternalSongsBatch(String ids) {
        log.debug("Internal: getSongsBatch ids={}", ids);
        List<UUID> uuids = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(UUID::fromString)
                .collect(Collectors.toList());
        List<Song> songs = songService.findByIds(uuids);
        return ResponseEntity.ok(songs.stream().map(this::toSongDto).collect(Collectors.toList()));
    }

    @Override
    public ResponseEntity<ArtistInternalDto> getInternalArtistById(UUID id) {
        log.debug("Internal: getArtist id={}", id);
        Artist artist = artistService.findById(id);
        return ResponseEntity.ok(toArtistDto(artist));
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private SongInternalDto toSongDto(Song song) {
        SongInternalDto dto = new SongInternalDto();
        dto.setId(song.getId());
        dto.setTitle(song.getTitle());
        dto.setArtistName(song.getArtist().getName());
        dto.setCoverUrl(song.getCoverUrl());
        dto.setAudioUrl(song.getAudioUrl());
        // Song.duration is in seconds — internal contract exposes milliseconds
        dto.setDurationMs((long) song.getDuration() * 1000L);
        return dto;
    }

    private ArtistInternalDto toArtistDto(Artist artist) {
        ArtistInternalDto dto = new ArtistInternalDto();
        dto.setId(artist.getId());
        dto.setName(artist.getName());
        dto.setPhotoUrl(artist.getPhotoUrl());
        dto.setMonthlyListeners(artist.getMonthlyListeners());
        return dto;
    }
}
