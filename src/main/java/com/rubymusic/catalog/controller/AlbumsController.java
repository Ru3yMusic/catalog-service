package com.rubymusic.catalog.controller;

import com.rubymusic.catalog.dto.AlbumPage;
import com.rubymusic.catalog.dto.AlbumRequest;
import com.rubymusic.catalog.dto.AlbumResponse;
import com.rubymusic.catalog.dto.SongPage;
import com.rubymusic.catalog.mapper.AlbumMapper;
import com.rubymusic.catalog.mapper.SongMapper;
import com.rubymusic.catalog.service.AlbumService;
import com.rubymusic.catalog.service.SongService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AlbumsController implements AlbumsApi {

    private final AlbumService albumService;
    private final SongService songService;
    private final AlbumMapper albumMapper;
    private final SongMapper songMapper;

    @Override
    public ResponseEntity<AlbumPage> listAlbums(UUID artistId, Integer page, Integer size) {
        var p = albumService.findAll(artistId, PageRequest.of(page, size));
        return ResponseEntity.ok(toPage(p));
    }

    @Override
    public ResponseEntity<AlbumResponse> createAlbum(AlbumRequest body) {
        AlbumResponse dto = albumMapper.toDto(
                albumService.create(body.getTitle(), body.getArtistId(),
                        body.getCoverUrl(), body.getReleaseDate(), body.getStationId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @Override
    public ResponseEntity<AlbumPage> getNewReleases(Integer page, Integer size) {
        var p = albumService.findNewReleases(PageRequest.of(page, size));
        return ResponseEntity.ok(toPage(p));
    }

    @Override
    public ResponseEntity<AlbumPage> getTopAlbums(Integer page, Integer size) {
        var p = albumService.findTopByStreams(PageRequest.of(page, size));
        return ResponseEntity.ok(toPage(p));
    }

    @Override
    public ResponseEntity<AlbumPage> searchAlbums(String q, Integer page, Integer size) {
        var p = albumService.search(q, PageRequest.of(page, size));
        return ResponseEntity.ok(toPage(p));
    }

    @Override
    public ResponseEntity<AlbumResponse> getAlbumById(UUID id) {
        return ResponseEntity.ok(albumMapper.toDto(albumService.findById(id)));
    }

    @Override
    public ResponseEntity<AlbumResponse> updateAlbum(UUID id, AlbumRequest body) {
        AlbumResponse dto = albumMapper.toDto(
                albumService.update(id, body.getTitle(), body.getCoverUrl(), body.getReleaseDate(), body.getStationId()));
        return ResponseEntity.ok(dto);
    }

    @Override
    public ResponseEntity<Void> deleteAlbum(UUID id) {
        albumService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<SongPage> getAlbumSongs(UUID id, Integer page, Integer size) {
        var p = songService.findByAlbumId(id, PageRequest.of(page, size));
        SongPage dto = new SongPage()
                .content(songMapper.toDtoList(p.getContent()))
                .totalElements((int) p.getTotalElements())
                .totalPages(p.getTotalPages())
                .page(p.getNumber())
                .size(p.getSize());
        return ResponseEntity.ok(dto);
    }

    private AlbumPage toPage(org.springframework.data.domain.Page<com.rubymusic.catalog.model.Album> p) {
        return new AlbumPage()
                .content(albumMapper.toDtoList(p.getContent()))
                .totalElements((int) p.getTotalElements())
                .totalPages(p.getTotalPages())
                .page(p.getNumber())
                .size(p.getSize());
    }
}
