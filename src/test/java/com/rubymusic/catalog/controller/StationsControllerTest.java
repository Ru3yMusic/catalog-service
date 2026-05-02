package com.rubymusic.catalog.controller;

import com.rubymusic.catalog.dto.SongResponse;
import com.rubymusic.catalog.dto.StationResponse;
import com.rubymusic.catalog.exception.GlobalExceptionHandler;
import com.rubymusic.catalog.mapper.SongMapper;
import com.rubymusic.catalog.mapper.StationMapper;
import com.rubymusic.catalog.model.Song;
import com.rubymusic.catalog.model.Station;
import com.rubymusic.catalog.service.SongService;
import com.rubymusic.catalog.service.StationService;
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
class StationsControllerTest {

    @Mock private StationService stationService;
    @Mock private SongService songService;
    @Mock private StationMapper stationMapper;
    @Mock private SongMapper songMapper;

    @InjectMocks
    private StationsController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ── listActiveStations ────────────────────────────────────────────────────

    @Test
    void listActiveStations_returns200_withList() throws Exception {
        when(stationService.findAllActive()).thenReturn(List.of(mock(Station.class), mock(Station.class)));
        when(stationMapper.toDtoList(anyList()))
                .thenReturn(List.of(new StationResponse(), new StationResponse()));

        mockMvc.perform(get("/api/v1/catalog/stations/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ── listStations ──────────────────────────────────────────────────────────

    @Test
    void listStations_returns200() throws Exception {
        Page<Station> page = new PageImpl<>(List.of(mock(Station.class)), PageRequest.of(0, 20), 1);
        when(stationService.findAll(any(PageRequest.class))).thenReturn(page);
        when(stationMapper.toDtoList(anyList())).thenReturn(List.of(new StationResponse()));

        mockMvc.perform(get("/api/v1/catalog/stations")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ── createStation ─────────────────────────────────────────────────────────

    @Test
    void createStation_withSongIds_returns201() throws Exception {
        UUID genreId = UUID.randomUUID();
        UUID s1 = UUID.randomUUID();
        UUID s2 = UUID.randomUUID();
        UUID s3 = UUID.randomUUID();
        Station created = mock(Station.class);
        when(stationService.create(eq("Chill"), eq(genreId), any(), any(), anySet()))
                .thenReturn(created);
        when(stationMapper.toDto(created)).thenReturn(new StationResponse());

        mockMvc.perform(post("/api/v1/catalog/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Chill\",\"genreId\":\"" + genreId + "\"," +
                                "\"gradientStart\":\"#111111\",\"gradientEnd\":\"#222222\"," +
                                "\"songIds\":[\"" + s1 + "\",\"" + s2 + "\",\"" + s3 + "\"]}"))
                .andExpect(status().isCreated());
    }

    @Test
    void createStation_invalidGradient_returns422() throws Exception {
        UUID genreId = UUID.randomUUID();
        UUID s1 = UUID.randomUUID();
        UUID s2 = UUID.randomUUID();
        UUID s3 = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/catalog/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Chill\",\"genreId\":\"" + genreId + "\"," +
                                "\"gradientStart\":\"#111\",\"gradientEnd\":\"#222\"," +
                                "\"songIds\":[\"" + s1 + "\",\"" + s2 + "\",\"" + s3 + "\"]}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void createStation_serviceRejects_returns400() throws Exception {
        UUID genreId = UUID.randomUUID();
        UUID s1 = UUID.randomUUID();
        UUID s2 = UUID.randomUUID();
        UUID s3 = UUID.randomUUID();
        when(stationService.create(any(), any(), any(), any(), anySet()))
                .thenThrow(new IllegalArgumentException("Station name already exists"));

        mockMvc.perform(post("/api/v1/catalog/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Chill\",\"genreId\":\"" + genreId + "\"," +
                                "\"gradientStart\":\"#111111\",\"gradientEnd\":\"#222222\"," +
                                "\"songIds\":[\"" + s1 + "\",\"" + s2 + "\",\"" + s3 + "\"]}"))
                .andExpect(status().isBadRequest());
    }

    // ── searchStations ────────────────────────────────────────────────────────

    @Test
    void searchStations_returns200() throws Exception {
        Page<Station> page = new PageImpl<>(List.of(mock(Station.class)), PageRequest.of(0, 10), 1);
        when(stationService.search(eq("chill"), any(PageRequest.class))).thenReturn(page);
        when(stationMapper.toDtoList(anyList())).thenReturn(List.of(new StationResponse()));

        mockMvc.perform(get("/api/v1/catalog/stations/search")
                        .param("q", "chill")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ── getStationById ────────────────────────────────────────────────────────

    @Test
    void getStationById_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        Station station = mock(Station.class);
        when(stationService.findById(id)).thenReturn(station);
        when(stationMapper.toDto(station)).thenReturn(new StationResponse());

        mockMvc.perform(get("/api/v1/catalog/stations/{id}", id))
                .andExpect(status().isOk());
    }

    @Test
    void getStationById_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(stationService.findById(id)).thenThrow(new NoSuchElementException("Station not found"));

        mockMvc.perform(get("/api/v1/catalog/stations/{id}", id))
                .andExpect(status().isNotFound());
    }

    // ── updateStation ─────────────────────────────────────────────────────────

    @Test
    void updateStation_withSongIds_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        UUID genreId = UUID.randomUUID();
        UUID s1 = UUID.randomUUID();
        UUID s2 = UUID.randomUUID();
        UUID s3 = UUID.randomUUID();
        Station updated = mock(Station.class);
        when(stationService.update(eq(id), eq("New"), eq(genreId), any(), any(), eq(true), anySet()))
                .thenReturn(updated);
        when(stationMapper.toDto(updated)).thenReturn(new StationResponse());

        mockMvc.perform(put("/api/v1/catalog/stations/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New\",\"genreId\":\"" + genreId + "\"," +
                                "\"gradientStart\":\"#111111\",\"gradientEnd\":\"#222222\"," +
                                "\"isActive\":true,\"songIds\":[\"" + s1 + "\",\"" + s2 + "\",\"" + s3 + "\"]}"))
                .andExpect(status().isOk());
    }

    // ── deleteStation ─────────────────────────────────────────────────────────

    @Test
    void deleteStation_returns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/catalog/stations/{id}", id))
                .andExpect(status().isNoContent());

        verify(stationService).delete(id);
    }

    // ── getSongsByStation ─────────────────────────────────────────────────────

    @Test
    void getSongsByStation_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        Page<Song> page = new PageImpl<>(List.of(mock(Song.class)), PageRequest.of(0, 10), 1);
        when(songService.findByStationId(eq(id), any(PageRequest.class))).thenReturn(page);
        when(songMapper.toDtoList(anyList())).thenReturn(List.of(new SongResponse()));

        mockMvc.perform(get("/api/v1/catalog/stations/{id}/songs", id)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
