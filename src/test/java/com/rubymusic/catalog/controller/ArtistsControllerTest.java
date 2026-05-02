package com.rubymusic.catalog.controller;

import com.rubymusic.catalog.dto.AlbumResponse;
import com.rubymusic.catalog.dto.ArtistResponse;
import com.rubymusic.catalog.dto.SongResponse;
import com.rubymusic.catalog.exception.GlobalExceptionHandler;
import com.rubymusic.catalog.mapper.AlbumMapper;
import com.rubymusic.catalog.mapper.ArtistMapper;
import com.rubymusic.catalog.mapper.SongMapper;
import com.rubymusic.catalog.model.Album;
import com.rubymusic.catalog.model.Artist;
import com.rubymusic.catalog.model.Song;
import com.rubymusic.catalog.service.AlbumService;
import com.rubymusic.catalog.service.ArtistService;
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
import static org.mockito.Mockito.doThrow;
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
class ArtistsControllerTest {

    @Mock private ArtistService artistService;
    @Mock private AlbumService albumService;
    @Mock private SongService songService;
    @Mock private ArtistMapper artistMapper;
    @Mock private AlbumMapper albumMapper;
    @Mock private SongMapper songMapper;

    @InjectMocks
    private ArtistsController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ── listArtists ───────────────────────────────────────────────────────────

    @Test
    void listArtists_returns200_withMappedPage() throws Exception {
        Artist a1 = mock(Artist.class);
        Page<Artist> page = new PageImpl<>(List.of(a1), PageRequest.of(0, 20), 1);

        when(artistService.findAll(eq(true), any(PageRequest.class))).thenReturn(page);
        when(artistMapper.toDtoList(anyList())).thenReturn(List.of(new ArtistResponse()));

        mockMvc.perform(get("/api/v1/catalog/artists")
                        .param("isTop", "true")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    // ── createArtist ──────────────────────────────────────────────────────────

    @Test
    void createArtist_returns201() throws Exception {
        Artist created = mock(Artist.class);
        when(artistService.create(eq("Drake"), eq("photo.jpg"), eq("bio"), eq(true)))
                .thenReturn(created);
        when(artistMapper.toDto(created)).thenReturn(new ArtistResponse());

        mockMvc.perform(post("/api/v1/catalog/artists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Drake\",\"photoUrl\":\"photo.jpg\",\"bio\":\"bio\",\"isTop\":true}"))
                .andExpect(status().isCreated());

        verify(artistService).create("Drake", "photo.jpg", "bio", true);
    }

    @Test
    void createArtist_serviceRejects_returns400() throws Exception {
        when(artistService.create(any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Artist name already exists"));

        mockMvc.perform(post("/api/v1/catalog/artists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Drake\",\"photoUrl\":\"x\",\"bio\":\"y\",\"isTop\":false}"))
                .andExpect(status().isBadRequest());
    }

    // ── getTopArtists ─────────────────────────────────────────────────────────

    @Test
    void getTopArtists_returns200_withList() throws Exception {
        when(artistService.findTopArtists()).thenReturn(List.of(mock(Artist.class), mock(Artist.class)));
        when(artistMapper.toDtoList(anyList()))
                .thenReturn(List.of(new ArtistResponse(), new ArtistResponse()));

        mockMvc.perform(get("/api/v1/catalog/artists/top"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ── searchArtists ─────────────────────────────────────────────────────────

    @Test
    void searchArtists_returns200() throws Exception {
        Page<Artist> page = new PageImpl<>(List.of(mock(Artist.class)), PageRequest.of(0, 10), 1);
        when(artistService.search(eq("dra"), any(PageRequest.class))).thenReturn(page);
        when(artistMapper.toDtoList(anyList())).thenReturn(List.of(new ArtistResponse()));

        mockMvc.perform(get("/api/v1/catalog/artists/search")
                        .param("q", "dra")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ── getArtistById ─────────────────────────────────────────────────────────

    @Test
    void getArtistById_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        Artist artist = mock(Artist.class);
        when(artistService.findById(id)).thenReturn(artist);
        when(artistMapper.toDto(artist)).thenReturn(new ArtistResponse());

        mockMvc.perform(get("/api/v1/catalog/artists/{id}", id))
                .andExpect(status().isOk());

        verify(artistService).findById(id);
    }

    @Test
    void getArtistById_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(artistService.findById(id)).thenThrow(new NoSuchElementException("Artist not found"));

        mockMvc.perform(get("/api/v1/catalog/artists/{id}", id))
                .andExpect(status().isNotFound());
    }

    // ── updateArtist ──────────────────────────────────────────────────────────

    @Test
    void updateArtist_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        Artist updated = mock(Artist.class);
        when(artistService.update(eq(id), eq("New"), any(), any(), any())).thenReturn(updated);
        when(artistMapper.toDto(updated)).thenReturn(new ArtistResponse());

        mockMvc.perform(put("/api/v1/catalog/artists/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New\",\"photoUrl\":\"p\",\"bio\":\"b\",\"isTop\":false}"))
                .andExpect(status().isOk());
    }

    // ── deleteArtist ──────────────────────────────────────────────────────────

    @Test
    void deleteArtist_returns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/catalog/artists/{id}", id))
                .andExpect(status().isNoContent());

        verify(artistService).delete(id);
    }

    @Test
    void deleteArtist_serviceFails_returns500() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new RuntimeException("DB error")).when(artistService).delete(id);

        mockMvc.perform(delete("/api/v1/catalog/artists/{id}", id))
                .andExpect(status().isInternalServerError());
    }

    // ── getArtistAlbums ───────────────────────────────────────────────────────

    @Test
    void getArtistAlbums_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        Page<Album> page = new PageImpl<>(List.of(mock(Album.class)), PageRequest.of(0, 5), 1);
        when(albumService.findByArtistId(eq(id), any(PageRequest.class))).thenReturn(page);
        when(albumMapper.toDtoList(anyList())).thenReturn(List.of(new AlbumResponse()));

        mockMvc.perform(get("/api/v1/catalog/artists/{id}/albums", id)
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ── getArtistSongs ────────────────────────────────────────────────────────

    @Test
    void getArtistSongs_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        Page<Song> page = new PageImpl<>(List.of(mock(Song.class)), PageRequest.of(0, 5), 1);
        when(songService.findByArtistId(eq(id), any(PageRequest.class))).thenReturn(page);
        when(songMapper.toDtoList(anyList())).thenReturn(List.of(new SongResponse()));

        mockMvc.perform(get("/api/v1/catalog/artists/{id}/songs", id)
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
