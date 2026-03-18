package com.rubymusic.catalog.controller;

import com.rubymusic.catalog.dto.AlbumPage;
import com.rubymusic.catalog.dto.ArtistPage;
import com.rubymusic.catalog.dto.ArtistRequest;
import com.rubymusic.catalog.dto.ArtistResponse;
import com.rubymusic.catalog.dto.SongPage;
import com.rubymusic.catalog.mapper.AlbumMapper;
import com.rubymusic.catalog.mapper.ArtistMapper;
import com.rubymusic.catalog.mapper.SongMapper;
import com.rubymusic.catalog.service.AlbumService;
import com.rubymusic.catalog.service.ArtistService;
import com.rubymusic.catalog.service.SongService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ArtistsController implements ArtistsApi {

    private final ArtistService artistService;
    private final AlbumService albumService;
    private final SongService songService;
    private final ArtistMapper artistMapper;
    private final AlbumMapper albumMapper;
    private final SongMapper songMapper;

    @Override
    public ResponseEntity<ArtistPage> listArtists(Integer page, Integer size) {
        var p = artistService.findAll(PageRequest.of(page, size));
        ArtistPage dto = new ArtistPage()
                .content(artistMapper.toDtoList(p.getContent()))
                .totalElements((int) p.getTotalElements())
                .totalPages(p.getTotalPages())
                .page(p.getNumber())
                .size(p.getSize());
        return ResponseEntity.ok(dto);
    }

    @Override
    public ResponseEntity<ArtistResponse> createArtist(ArtistRequest body) {
        ArtistResponse dto = artistMapper.toDto(
                artistService.create(body.getName(), body.getPhotoUrl(), body.getBio()));
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @Override
    public ResponseEntity<List<ArtistResponse>> getTopArtists() {
        return ResponseEntity.ok(artistMapper.toDtoList(artistService.findTopArtists()));
    }

    @Override
    public ResponseEntity<ArtistPage> searchArtists(String q, Integer page, Integer size) {
        var p = artistService.search(q, PageRequest.of(page, size));
        ArtistPage dto = new ArtistPage()
                .content(artistMapper.toDtoList(p.getContent()))
                .totalElements((int) p.getTotalElements())
                .totalPages(p.getTotalPages())
                .page(p.getNumber())
                .size(p.getSize());
        return ResponseEntity.ok(dto);
    }

    @Override
    public ResponseEntity<ArtistResponse> getArtistById(UUID id) {
        return ResponseEntity.ok(artistMapper.toDto(artistService.findById(id)));
    }

    @Override
    public ResponseEntity<ArtistResponse> updateArtist(UUID id, ArtistRequest body) {
        ArtistResponse dto = artistMapper.toDto(
                artistService.update(id, body.getName(), body.getPhotoUrl(), body.getBio(), body.getIsTop()));
        return ResponseEntity.ok(dto);
    }

    @Override
    public ResponseEntity<Void> deleteArtist(UUID id) {
        artistService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<AlbumPage> getArtistAlbums(UUID id, Integer page, Integer size) {
        var p = albumService.findByArtistId(id, PageRequest.of(page, size));
        AlbumPage dto = new AlbumPage()
                .content(albumMapper.toDtoList(p.getContent()))
                .totalElements((int) p.getTotalElements())
                .totalPages(p.getTotalPages())
                .page(p.getNumber())
                .size(p.getSize());
        return ResponseEntity.ok(dto);
    }

    @Override
    public ResponseEntity<SongPage> getArtistSongs(UUID id, Integer page, Integer size) {
        var p = songService.findByArtistId(id, PageRequest.of(page, size));
        SongPage dto = new SongPage()
                .content(songMapper.toDtoList(p.getContent()))
                .totalElements((int) p.getTotalElements())
                .totalPages(p.getTotalPages())
                .page(p.getNumber())
                .size(p.getSize());
        return ResponseEntity.ok(dto);
    }
}
