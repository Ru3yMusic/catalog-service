package com.rubymusic.catalog.controller;

import com.rubymusic.catalog.dto.GenreResponse;
import com.rubymusic.catalog.dto.SongResponse;
import com.rubymusic.catalog.exception.GlobalExceptionHandler;
import com.rubymusic.catalog.mapper.GenreMapper;
import com.rubymusic.catalog.mapper.SongMapper;
import com.rubymusic.catalog.model.Genre;
import com.rubymusic.catalog.model.Song;
import com.rubymusic.catalog.service.GenreService;
import com.rubymusic.catalog.service.SongService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GenresControllerTest {

    @Mock private GenreService genreService;
    @Mock private SongService songService;
    @Mock private GenreMapper genreMapper;
    @Mock private SongMapper songMapper;

    @InjectMocks
    private GenresController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ── listGenres ────────────────────────────────────────────────────────────

    @Test
    void listGenres_withoutQuery_returns200() throws Exception {
        when(genreService.findAll(null)).thenReturn(List.of(mock(Genre.class), mock(Genre.class)));
        when(genreMapper.toDtoList(anyList()))
                .thenReturn(List.of(new GenreResponse(), new GenreResponse()));

        mockMvc.perform(get("/api/v1/catalog/genres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void listGenres_withQuery_returns200() throws Exception {
        when(genreService.findAll("rock")).thenReturn(List.of(mock(Genre.class)));
        when(genreMapper.toDtoList(anyList())).thenReturn(List.of(new GenreResponse()));

        mockMvc.perform(get("/api/v1/catalog/genres").param("q", "rock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ── createGenre ───────────────────────────────────────────────────────────

    @Test
    void createGenre_returns201() throws Exception {
        Genre created = mock(Genre.class);
        when(genreService.create(eq("Rock"), eq("#000000"), eq("#FFFFFF"))).thenReturn(created);
        when(genreMapper.toDto(created)).thenReturn(new GenreResponse());

        mockMvc.perform(post("/api/v1/catalog/genres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Rock\",\"gradientStart\":\"#000000\",\"gradientEnd\":\"#FFFFFF\"}"))
                .andExpect(status().isCreated());

        verify(genreService).create("Rock", "#000000", "#FFFFFF");
    }

    @Test
    void createGenre_duplicate_returns409() throws Exception {
        when(genreService.create(any(), any(), any()))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        mockMvc.perform(post("/api/v1/catalog/genres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Rock\",\"gradientStart\":\"#000000\",\"gradientEnd\":\"#FFFFFF\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void createGenre_invalidGradient_returns422() throws Exception {
        mockMvc.perform(post("/api/v1/catalog/genres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Rock\",\"gradientStart\":\"#000\",\"gradientEnd\":\"#FFF\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    // ── getGenreById ──────────────────────────────────────────────────────────

    @Test
    void getGenreById_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        Genre genre = mock(Genre.class);
        when(genreService.findById(id)).thenReturn(genre);
        when(genreMapper.toDto(genre)).thenReturn(new GenreResponse());

        mockMvc.perform(get("/api/v1/catalog/genres/{id}", id))
                .andExpect(status().isOk());
    }

    @Test
    void getGenreById_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(genreService.findById(id)).thenThrow(new NoSuchElementException("Genre not found"));

        mockMvc.perform(get("/api/v1/catalog/genres/{id}", id))
                .andExpect(status().isNotFound());
    }

    // ── updateGenre ───────────────────────────────────────────────────────────

    @Test
    void updateGenre_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        Genre updated = mock(Genre.class);
        when(genreService.update(eq(id), eq("Pop"), any(), any())).thenReturn(updated);
        when(genreMapper.toDto(updated)).thenReturn(new GenreResponse());

        mockMvc.perform(put("/api/v1/catalog/genres/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Pop\",\"gradientStart\":\"#111111\",\"gradientEnd\":\"#222222\"}"))
                .andExpect(status().isOk());
    }

    // ── deleteGenre ───────────────────────────────────────────────────────────

    @Test
    void deleteGenre_returns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/catalog/genres/{id}", id))
                .andExpect(status().isNoContent());

        verify(genreService).delete(id);
    }

    // ── getSongsByGenre ───────────────────────────────────────────────────────

    @Test
    void getSongsByGenre_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        Page<Song> page = new PageImpl<>(List.of(mock(Song.class)), PageRequest.of(0, 10), 1);
        when(songService.findByGenreId(eq(id), any(PageRequest.class))).thenReturn(page);
        when(songMapper.toDtoList(anyList())).thenReturn(List.of(new SongResponse()));

        mockMvc.perform(get("/api/v1/catalog/genres/{id}/songs", id)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
