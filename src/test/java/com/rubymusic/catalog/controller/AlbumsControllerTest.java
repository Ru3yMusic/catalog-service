package com.rubymusic.catalog.controller;

import com.rubymusic.catalog.dto.AlbumResponse;
import com.rubymusic.catalog.dto.SongResponse;
import com.rubymusic.catalog.exception.GlobalExceptionHandler;
import com.rubymusic.catalog.mapper.AlbumMapper;
import com.rubymusic.catalog.mapper.SongMapper;
import com.rubymusic.catalog.model.Album;
import com.rubymusic.catalog.model.Song;
import com.rubymusic.catalog.service.AlbumService;
import com.rubymusic.catalog.service.SongService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
class AlbumsControllerTest {

    @Mock private AlbumService albumService;
    @Mock private SongService songService;
    @Mock private AlbumMapper albumMapper;
    @Mock private SongMapper songMapper;

    @InjectMocks
    private AlbumsController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ── listAlbums ────────────────────────────────────────────────────────────

    @Test
    void listAlbums_withArtistId_returns200() throws Exception {
        UUID artistId = UUID.randomUUID();
        Page<Album> page = new PageImpl<>(List.of(mock(Album.class)), PageRequest.of(0, 20), 1);
        when(albumService.findAll(eq(artistId), any(PageRequest.class))).thenReturn(page);
        when(albumMapper.toDtoList(anyList())).thenReturn(List.of(new AlbumResponse()));

        mockMvc.perform(get("/api/v1/catalog/albums")
                        .param("artistId", artistId.toString())
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listAlbums_noArtistFilter_returns200() throws Exception {
        Page<Album> page = new PageImpl<>(List.of(mock(Album.class)), PageRequest.of(0, 20), 1);
        when(albumService.findAll(any(), any(PageRequest.class))).thenReturn(page);
        when(albumMapper.toDtoList(anyList())).thenReturn(List.of(new AlbumResponse()));

        mockMvc.perform(get("/api/v1/catalog/albums")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk());
    }

    // ── createAlbum ───────────────────────────────────────────────────────────

    @Test
    void createAlbum_returns201() throws Exception {
        UUID artistId = UUID.randomUUID();
        UUID stationId = UUID.randomUUID();
        Album created = mock(Album.class);
        when(albumService.create(eq("Album"), eq(artistId), any(), any(), eq(stationId)))
                .thenReturn(created);
        when(albumMapper.toDto(created)).thenReturn(new AlbumResponse());

        mockMvc.perform(post("/api/v1/catalog/albums")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Album\",\"artistId\":\"" + artistId + "\"," +
                                "\"coverUrl\":\"c\",\"releaseDateTime\":\"2026-01-01T10:00:00\"," +
                                "\"stationId\":\"" + stationId + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void createAlbum_artistNotFound_returns404() throws Exception {
        when(albumService.create(any(), any(), any(), any(), any()))
                .thenThrow(new NoSuchElementException("Artist not found"));

        mockMvc.perform(post("/api/v1/catalog/albums")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Album\",\"artistId\":\"" + UUID.randomUUID() + "\"," +
                                "\"coverUrl\":\"c\",\"releaseDateTime\":\"2026-01-01T10:00:00\"," +
                                "\"stationId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isNotFound());
    }

    // ── getNewReleases ────────────────────────────────────────────────────────

    @Test
    void getNewReleases_returns200() throws Exception {
        Page<Album> page = new PageImpl<>(List.of(mock(Album.class), mock(Album.class)),
                PageRequest.of(0, 10), 2);
        when(albumService.findNewReleases(any(PageRequest.class))).thenReturn(page);
        when(albumMapper.toDtoList(anyList()))
                .thenReturn(List.of(new AlbumResponse(), new AlbumResponse()));

        mockMvc.perform(get("/api/v1/catalog/albums/new-releases")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    // ── getTopAlbums ──────────────────────────────────────────────────────────

    @Test
    void getTopAlbums_returns200() throws Exception {
        Page<Album> page = new PageImpl<>(List.of(mock(Album.class)), PageRequest.of(0, 10), 1);
        when(albumService.findTopByStreams(any(PageRequest.class))).thenReturn(page);
        when(albumMapper.toDtoList(anyList())).thenReturn(List.of(new AlbumResponse()));

        mockMvc.perform(get("/api/v1/catalog/albums/top")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ── searchAlbums ──────────────────────────────────────────────────────────

    @Test
    void searchAlbums_returns200() throws Exception {
        Page<Album> page = new PageImpl<>(List.of(mock(Album.class)), PageRequest.of(0, 10), 1);
        when(albumService.search(eq("alb"), any(PageRequest.class))).thenReturn(page);
        when(albumMapper.toDtoList(anyList())).thenReturn(List.of(new AlbumResponse()));

        mockMvc.perform(get("/api/v1/catalog/albums/search")
                        .param("q", "alb")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ── getAlbumById ──────────────────────────────────────────────────────────

    @Test
    void getAlbumById_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        Album album = mock(Album.class);
        when(albumService.findById(id)).thenReturn(album);
        when(albumMapper.toDto(album)).thenReturn(new AlbumResponse());

        mockMvc.perform(get("/api/v1/catalog/albums/{id}", id))
                .andExpect(status().isOk());
    }

    @Test
    void getAlbumById_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(albumService.findById(id)).thenThrow(new NoSuchElementException("Album not found"));

        mockMvc.perform(get("/api/v1/catalog/albums/{id}", id))
                .andExpect(status().isNotFound());
    }

    // ── updateAlbum ───────────────────────────────────────────────────────────

    @Test
    void updateAlbum_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        UUID stationId = UUID.randomUUID();
        Album updated = mock(Album.class);
        when(albumService.update(eq(id), eq("New"), any(), any(), eq(stationId))).thenReturn(updated);
        when(albumMapper.toDto(updated)).thenReturn(new AlbumResponse());

        mockMvc.perform(put("/api/v1/catalog/albums/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"New\",\"artistId\":\"" + UUID.randomUUID() + "\"," +
                                "\"coverUrl\":\"c\",\"releaseDateTime\":\"2026-01-01T10:00:00\"," +
                                "\"stationId\":\"" + stationId + "\"}"))
                .andExpect(status().isOk());
    }

    // ── deleteAlbum ───────────────────────────────────────────────────────────

    @Test
    void deleteAlbum_returns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/catalog/albums/{id}", id))
                .andExpect(status().isNoContent());

        verify(albumService).delete(id);
    }

    // ── getAlbumSongs ─────────────────────────────────────────────────────────

    @Test
    void getAlbumSongs_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        Page<Song> page = new PageImpl<>(List.of(mock(Song.class)), PageRequest.of(0, 10), 1);
        when(songService.findByAlbumId(eq(id), any(PageRequest.class))).thenReturn(page);
        when(songMapper.toDtoList(anyList())).thenReturn(List.of(new SongResponse()));

        mockMvc.perform(get("/api/v1/catalog/albums/{id}/songs", id)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
