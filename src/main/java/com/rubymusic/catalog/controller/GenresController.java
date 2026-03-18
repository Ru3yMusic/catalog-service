package com.rubymusic.catalog.controller;

import com.rubymusic.catalog.dto.GenreRequest;
import com.rubymusic.catalog.dto.GenreResponse;
import com.rubymusic.catalog.dto.SongPage;
import com.rubymusic.catalog.mapper.GenreMapper;
import com.rubymusic.catalog.mapper.SongMapper;
import com.rubymusic.catalog.service.GenreService;
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
public class GenresController implements GenresApi {

    private final GenreService genreService;
    private final SongService songService;
    private final GenreMapper genreMapper;
    private final SongMapper songMapper;

    @Override
    public ResponseEntity<List<GenreResponse>> listGenres() {
        return ResponseEntity.ok(genreMapper.toDtoList(genreService.findAll()));
    }

    @Override
    public ResponseEntity<GenreResponse> createGenre(GenreRequest body) {
        GenreResponse dto = genreMapper.toDto(
                genreService.create(body.getName(), body.getGradientStart(), body.getGradientEnd()));
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @Override
    public ResponseEntity<GenreResponse> getGenreById(UUID id) {
        return ResponseEntity.ok(genreMapper.toDto(genreService.findById(id)));
    }

    @Override
    public ResponseEntity<GenreResponse> updateGenre(UUID id, GenreRequest body) {
        GenreResponse dto = genreMapper.toDto(
                genreService.update(id, body.getName(), body.getGradientStart(), body.getGradientEnd()));
        return ResponseEntity.ok(dto);
    }

    @Override
    public ResponseEntity<Void> deleteGenre(UUID id) {
        genreService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<SongPage> getSongsByGenre(UUID id, Integer page, Integer size) {
        var songPage = songService.findByGenreId(id, PageRequest.of(page, size));
        SongPage dto = new SongPage()
                .content(songMapper.toDtoList(songPage.getContent()))
                .totalElements((int) songPage.getTotalElements())
                .totalPages(songPage.getTotalPages())
                .page(songPage.getNumber())
                .size(songPage.getSize());
        return ResponseEntity.ok(dto);
    }
}
