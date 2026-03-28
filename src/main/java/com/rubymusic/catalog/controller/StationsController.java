package com.rubymusic.catalog.controller;

import com.rubymusic.catalog.dto.SongPage;
import com.rubymusic.catalog.dto.StationPage;
import com.rubymusic.catalog.dto.StationRequest;
import com.rubymusic.catalog.dto.StationResponse;
import com.rubymusic.catalog.mapper.SongMapper;
import com.rubymusic.catalog.mapper.StationMapper;
import com.rubymusic.catalog.model.Station;
import com.rubymusic.catalog.service.SongService;
import com.rubymusic.catalog.service.StationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class StationsController implements StationsApi {

    private final StationService stationService;
    private final SongService songService;
    private final StationMapper stationMapper;
    private final SongMapper songMapper;

    @Override
    public ResponseEntity<List<StationResponse>> listActiveStations() {
        return ResponseEntity.ok(stationMapper.toDtoList(stationService.findAllActive()));
    }

    @Override
    public ResponseEntity<StationPage> listStations(Integer page, Integer size) {
        return ResponseEntity.ok(toPage(stationService.findAll(PageRequest.of(page, size))));
    }

    @Override
    public ResponseEntity<StationResponse> createStation(StationRequest body) {
        var songIds = body.getSongIds() == null ? new HashSet<UUID>() : new HashSet<>(body.getSongIds());
        StationResponse dto = stationMapper.toDto(stationService.create(
                body.getName(), body.getGenreId(),
                body.getGradientStart(), body.getGradientEnd(), songIds));
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @Override
    public ResponseEntity<StationPage> searchStations(String q, Integer page, Integer size) {
        return ResponseEntity.ok(toPage(stationService.search(q, PageRequest.of(page, size))));
    }

    @Override
    public ResponseEntity<StationResponse> getStationById(UUID id) {
        return ResponseEntity.ok(stationMapper.toDto(stationService.findById(id)));
    }

    @Override
    public ResponseEntity<StationResponse> updateStation(UUID id, StationRequest body) {
        var songIds = body.getSongIds() == null ? null : new HashSet<>(body.getSongIds());
        StationResponse dto = stationMapper.toDto(stationService.update(
                id, body.getName(), body.getGenreId(),
                body.getGradientStart(), body.getGradientEnd(), body.getIsActive(), songIds));
        return ResponseEntity.ok(dto);
    }

    @Override
    public ResponseEntity<Void> deleteStation(UUID id) {
        stationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<SongPage> getSongsByStation(UUID id, Integer page, Integer size) {
        var songPage = songService.findByStationId(id, PageRequest.of(page, size));
        SongPage dto = new SongPage()
                .content(songMapper.toDtoList(songPage.getContent()))
                .totalElements((int) songPage.getTotalElements())
                .totalPages(songPage.getTotalPages())
                .page(songPage.getNumber())
                .size(songPage.getSize());
        return ResponseEntity.ok(dto);
    }

    private StationPage toPage(Page<Station> p) {
        return new StationPage()
                .content(stationMapper.toDtoList(p.getContent()))
                .totalElements((int) p.getTotalElements())
                .totalPages(p.getTotalPages())
                .page(p.getNumber())
                .size(p.getSize());
    }
}
