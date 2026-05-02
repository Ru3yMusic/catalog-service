package com.rubymusic.catalog.controller;

import com.rubymusic.catalog.dto.SongResponse;
import com.rubymusic.catalog.exception.GlobalExceptionHandler;
import com.rubymusic.catalog.mapper.SongMapper;
import com.rubymusic.catalog.model.Song;
import com.rubymusic.catalog.repository.AlbumRepository;
import com.rubymusic.catalog.repository.ArtistRepository;
import com.rubymusic.catalog.repository.SongRepository;
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
import static org.mockito.ArgumentMatchers.anySet;
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
class SongsControllerTest {

    @Mock private SongService songService;
    @Mock private SongMapper songMapper;
    @Mock private SongRepository songRepository;
    @Mock private ArtistRepository artistRepository;
    @Mock private AlbumRepository albumRepository;

    @InjectMocks
    private SongsController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ── listSongs ─────────────────────────────────────────────────────────────

    @Test
    void listSongs_returns200_withMappedPage() throws Exception {
        Page<Song> page = new PageImpl<>(List.of(mock(Song.class)), PageRequest.of(0, 20), 1);
        when(songService.findAll(any(PageRequest.class))).thenReturn(page);
        when(songMapper.toDtoList(anyList())).thenReturn(List.of(new SongResponse()));

        mockMvc.perform(get("/api/v1/catalog/songs")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    // ── searchSongs ───────────────────────────────────────────────────────────

    @Test
    void searchSongs_returns200() throws Exception {
        Page<Song> page = new PageImpl<>(List.of(mock(Song.class)), PageRequest.of(0, 10), 1);
        when(songService.search(eq("track"), any(PageRequest.class))).thenReturn(page);
        when(songMapper.toDtoList(anyList())).thenReturn(List.of(new SongResponse()));

        mockMvc.perform(get("/api/v1/catalog/songs/search")
                        .param("q", "track")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ── getRecommendations ────────────────────────────────────────────────────

    @Test
    void getRecommendations_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        Page<Song> page = new PageImpl<>(List.of(mock(Song.class)), PageRequest.of(0, 10), 1);
        when(songService.findRecommendations(anyList(), any(PageRequest.class))).thenReturn(page);
        when(songMapper.toDtoList(anyList())).thenReturn(List.of(new SongResponse()));

        mockMvc.perform(get("/api/v1/catalog/songs/recommendations")
                        .param("songIds", id.toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ── getSongById ───────────────────────────────────────────────────────────

    @Test
    void getSongById_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        Song song = mock(Song.class);
        when(songService.findById(id)).thenReturn(song);
        when(songMapper.toDto(song)).thenReturn(new SongResponse());

        mockMvc.perform(get("/api/v1/catalog/songs/{id}", id))
                .andExpect(status().isOk());

        verify(songService).findById(id);
    }

    @Test
    void getSongById_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(songService.findById(id)).thenThrow(new NoSuchElementException("Song not found"));

        mockMvc.perform(get("/api/v1/catalog/songs/{id}", id))
                .andExpect(status().isNotFound());
    }

    // ── updateSong ────────────────────────────────────────────────────────────

    @Test
    void updateSong_withGenreIds_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        UUID genreId = UUID.randomUUID();
        Song updated = mock(Song.class);
        when(songService.update(eq(id), eq("New Title"), any(), any(), any(), any(), anySet()))
                .thenReturn(updated);
        when(songMapper.toDto(updated)).thenReturn(new SongResponse());

        mockMvc.perform(put("/api/v1/catalog/songs/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"New Title\",\"coverUrl\":\"c\",\"audioUrl\":\"a\"," +
                                "\"duration\":200,\"lyrics\":\"l\",\"genreIds\":[\"" + genreId + "\"]}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateSong_withNullGenreIds_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        Song updated = mock(Song.class);
        when(songService.update(eq(id), eq("New"), any(), any(), any(), any(), anySet()))
                .thenReturn(updated);
        when(songMapper.toDto(updated)).thenReturn(new SongResponse());

        mockMvc.perform(put("/api/v1/catalog/songs/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"New\",\"coverUrl\":\"c\",\"audioUrl\":\"a\"," +
                                "\"duration\":200,\"lyrics\":\"l\"}"))
                .andExpect(status().isOk());
    }

    // ── deleteSong ────────────────────────────────────────────────────────────

    @Test
    void deleteSong_returns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/catalog/songs/{id}", id))
                .andExpect(status().isNoContent());

        verify(songService).delete(id);
    }

    // ── createSong ────────────────────────────────────────────────────────────

    @Test
    void createSong_withGenreIds_returns201() throws Exception {
        UUID artistId = UUID.randomUUID();
        UUID albumId = UUID.randomUUID();
        UUID genreId = UUID.randomUUID();
        Song created = mock(Song.class);
        when(songService.create(eq("Song"), eq(artistId), eq(albumId),
                any(), any(), any(), any(), anySet())).thenReturn(created);
        when(songMapper.toDto(created)).thenReturn(new SongResponse());

        mockMvc.perform(post("/api/v1/catalog/songs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Song\",\"artistId\":\"" + artistId + "\"," +
                                "\"albumId\":\"" + albumId + "\",\"coverUrl\":\"c\"," +
                                "\"audioUrl\":\"a\",\"duration\":180,\"lyrics\":\"l\"," +
                                "\"genreIds\":[\"" + genreId + "\"]}"))
                .andExpect(status().isCreated());
    }

    @Test
    void createSong_withNullGenreIds_returns201() throws Exception {
        UUID artistId = UUID.randomUUID();
        UUID albumId = UUID.randomUUID();
        Song created = mock(Song.class);
        when(songService.create(eq("Song"), eq(artistId), eq(albumId),
                any(), any(), any(), any(), anySet())).thenReturn(created);
        when(songMapper.toDto(created)).thenReturn(new SongResponse());

        mockMvc.perform(post("/api/v1/catalog/songs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Song\",\"artistId\":\"" + artistId + "\"," +
                                "\"albumId\":\"" + albumId + "\",\"coverUrl\":\"c\"," +
                                "\"audioUrl\":\"a\",\"duration\":180,\"lyrics\":\"l\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void createSong_artistNotFound_returns404() throws Exception {
        UUID artistId = UUID.randomUUID();
        UUID albumId = UUID.randomUUID();
        when(songService.create(any(), any(), any(), any(), any(), any(), any(), anySet()))
                .thenThrow(new NoSuchElementException("Artist not found"));

        mockMvc.perform(post("/api/v1/catalog/songs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Song\",\"artistId\":\"" + artistId + "\"," +
                                "\"albumId\":\"" + albumId + "\",\"coverUrl\":\"c\"," +
                                "\"audioUrl\":\"a\",\"duration\":180,\"lyrics\":\"l\"}"))
                .andExpect(status().isNotFound());
    }

    // ── getRecentSongs (non-OpenAPI endpoint) ─────────────────────────────────

    @Test
    void getRecentSongs_returns200_withList() throws Exception {
        when(songService.findRecent()).thenReturn(List.of(mock(Song.class), mock(Song.class)));
        when(songMapper.toDtoList(anyList()))
                .thenReturn(List.of(new SongResponse(), new SongResponse()));

        mockMvc.perform(get("/api/v1/catalog/songs/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ── getCatalogStats (non-OpenAPI endpoint) ────────────────────────────────

    @Test
    void getCatalogStats_returns200_withCounts() throws Exception {
        when(songRepository.count()).thenReturn(150L);
        when(artistRepository.count()).thenReturn(20L);
        when(albumRepository.count()).thenReturn(35L);

        mockMvc.perform(get("/api/v1/catalog/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSongs").value(150))
                .andExpect(jsonPath("$.totalArtists").value(20))
                .andExpect(jsonPath("$.totalAlbums").value(35));
    }
}
