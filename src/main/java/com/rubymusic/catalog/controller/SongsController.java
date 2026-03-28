package com.rubymusic.catalog.controller;

import com.rubymusic.catalog.dto.SongCreateRequest;
import com.rubymusic.catalog.dto.SongPage;
import com.rubymusic.catalog.dto.SongResponse;
import com.rubymusic.catalog.dto.SongUpdateRequest;
import com.rubymusic.catalog.mapper.SongMapper;
import com.rubymusic.catalog.service.SongService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class SongsController implements SongsApi {

    private final SongService songService;
    private final SongMapper songMapper;

    @Override
    public ResponseEntity<SongPage> listSongs(Integer page, Integer size) {
        var p = songService.findAll(PageRequest.of(page, size));
        return ResponseEntity.ok(toPage(p));
    }

    @Override
    public ResponseEntity<SongPage> searchSongs(String q, Integer page, Integer size) {
        var p = songService.search(q, PageRequest.of(page, size));
        return ResponseEntity.ok(toPage(p));
    }

    @Override
    public ResponseEntity<SongPage> getRecommendations(List<UUID> songIds, Integer page, Integer size) {
        var p = songService.findRecommendations(songIds, PageRequest.of(page, size));
        return ResponseEntity.ok(toPage(p));
    }

    @Override
    public ResponseEntity<SongResponse> getSongById(UUID id) {
        return ResponseEntity.ok(songMapper.toDto(songService.findById(id)));
    }

    @Override
    public ResponseEntity<SongResponse> updateSong(UUID id, SongUpdateRequest body) {
        var genreIds = body.getGenreIds() != null ? new HashSet<>(body.getGenreIds()) : new HashSet<UUID>();
        SongResponse dto = songMapper.toDto(
                songService.update(id, body.getTitle(), body.getCoverUrl(),
                        body.getAudioUrl(), body.getLyrics(), genreIds));
        return ResponseEntity.ok(dto);
    }

    @Override
    public ResponseEntity<Void> deleteSong(UUID id) {
        songService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<SongResponse> createSong(SongCreateRequest body) {
        var genreIds = body.getGenreIds() != null ? new HashSet<>(body.getGenreIds()) : new HashSet<UUID>();
        SongResponse dto = songMapper.toDto(
                songService.create(body.getTitle(), body.getArtistId(), body.getAlbumId(),
                        body.getCoverUrl(), body.getAudioUrl(), body.getDuration(),
                        body.getLyrics(), genreIds));
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    private SongPage toPage(org.springframework.data.domain.Page<com.rubymusic.catalog.model.Song> p) {
        return new SongPage()
                .content(songMapper.toDtoList(p.getContent()))
                .totalElements((int) p.getTotalElements())
                .totalPages(p.getTotalPages())
                .page(p.getNumber())
                .size(p.getSize());
    }
}
