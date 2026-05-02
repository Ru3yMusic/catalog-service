package com.rubymusic.catalog.service.impl;

import com.rubymusic.catalog.model.Genre;
import com.rubymusic.catalog.model.Song;
import com.rubymusic.catalog.model.Station;
import com.rubymusic.catalog.repository.GenreRepository;
import com.rubymusic.catalog.repository.SongRepository;
import com.rubymusic.catalog.repository.StationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StationServiceImplTest {

    @Mock
    private StationRepository stationRepository;

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private SongRepository songRepository;

    @InjectMocks
    private StationServiceImpl service;

    // ── findAllActive ─────────────────────────────────────────────────────────

    @Test
    void findAllActive_delegatesToRepository() {
        when(stationRepository.findAllByIsActiveTrueOrderByNameAsc())
                .thenReturn(List.of(Station.builder().name("Rock").build()));

        List<Station> result = service.findAllActive();

        assertThat(result).hasSize(1);
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    void findAll_delegatesToRepository() {
        Pageable pageable = Pageable.ofSize(10);
        when(stationRepository.findAll(pageable)).thenReturn(Page.empty());

        service.findAll(pageable);

        verify(stationRepository).findAll(pageable);
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    void findById_existing_returnsStation() {
        UUID id = UUID.randomUUID();
        Station s = Station.builder().name("Rock").build();
        when(stationRepository.findById(id)).thenReturn(Optional.of(s));

        Station result = service.findById(id);

        assertThat(result).isSameAs(s);
    }

    @Test
    void findById_notFound_throwsIllegalArgument() {
        UUID id = UUID.randomUUID();
        when(stationRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Station not found");
    }

    // ── search ────────────────────────────────────────────────────────────────

    @Test
    void search_delegatesToRepository() {
        Pageable pageable = Pageable.ofSize(10);
        when(stationRepository.findByNameContainingIgnoreCaseAndIsActiveTrue("rock", pageable))
                .thenReturn(Page.empty());

        service.search("rock", pageable);

        verify(stationRepository).findByNameContainingIgnoreCaseAndIsActiveTrue("rock", pageable);
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_validRequest_persists() {
        UUID genreId = UUID.randomUUID();
        UUID s1 = UUID.randomUUID();
        UUID s2 = UUID.randomUUID();
        Set<UUID> songIds = Set.of(s1, s2);
        Genre genre = Genre.builder().name("Rock").build();
        Song song1 = Song.builder().title("S1").build();
        Song song2 = Song.builder().title("S2").build();

        when(stationRepository.existsByNameIgnoreCase("Rock Classics")).thenReturn(false);
        when(genreRepository.findById(genreId)).thenReturn(Optional.of(genre));
        when(songRepository.findAllById(songIds)).thenReturn(List.of(song1, song2));
        when(stationRepository.save(any(Station.class))).thenAnswer(inv -> inv.getArgument(0));

        Station result = service.create("Rock Classics", genreId, "#000", "#FFF", songIds);

        assertThat(result.getName()).isEqualTo("Rock Classics");
        assertThat(result.getGenre()).isSameAs(genre);
        assertThat(result.getSongs()).hasSize(2);
    }

    @Test
    void create_duplicateName_throwsIllegalArgument() {
        UUID genreId = UUID.randomUUID();
        when(stationRepository.existsByNameIgnoreCase("Rock")).thenReturn(true);

        assertThatThrownBy(() -> service.create("Rock", genreId, "#000", "#FFF", Set.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Station already exists");
    }

    @Test
    void create_unknownGenre_throwsIllegalArgument() {
        UUID genreId = UUID.randomUUID();
        when(stationRepository.existsByNameIgnoreCase("Rock")).thenReturn(false);
        when(genreRepository.findById(genreId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create("Rock", genreId, "#000", "#FFF", Set.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Genre not found");
    }

    @Test
    void create_lessThanTwoSongs_throwsIllegalArgument() {
        UUID genreId = UUID.randomUUID();
        UUID s1 = UUID.randomUUID();
        Genre genre = Genre.builder().name("Rock").build();
        when(stationRepository.existsByNameIgnoreCase("Rock")).thenReturn(false);
        when(genreRepository.findById(genreId)).thenReturn(Optional.of(genre));
        when(songRepository.findAllById(Set.of(s1)))
                .thenReturn(List.of(Song.builder().title("S1").build()));

        assertThatThrownBy(() -> service.create("Rock", genreId, "#000", "#FFF", Set.of(s1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 2 songs");
    }

    @Test
    void create_invalidSongIds_throwsIllegalArgument() {
        UUID genreId = UUID.randomUUID();
        UUID s1 = UUID.randomUUID();
        UUID s2 = UUID.randomUUID();
        Set<UUID> songIds = Set.of(s1, s2);
        Genre genre = Genre.builder().name("Rock").build();
        when(stationRepository.existsByNameIgnoreCase("Rock")).thenReturn(false);
        when(genreRepository.findById(genreId)).thenReturn(Optional.of(genre));
        // Only one of the two song IDs resolves
        when(songRepository.findAllById(songIds)).thenReturn(List.of(Song.builder().build()));

        assertThatThrownBy(() -> service.create("Rock", genreId, "#000", "#FFF", songIds))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("song IDs are invalid");
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void update_validFields_updatesAndSaves() {
        UUID id = UUID.randomUUID();
        Station existing = Station.builder()
                .name("Old")
                .gradientStart("#000")
                .gradientEnd("#FFF")
                .isActive(true)
                .build();
        when(stationRepository.findById(id)).thenReturn(Optional.of(existing));
        when(stationRepository.save(any(Station.class))).thenAnswer(inv -> inv.getArgument(0));

        Station result = service.update(id, "New", null, "#111", "#222", false, null);

        assertThat(result.getName()).isEqualTo("New");
        assertThat(result.getGradientStart()).isEqualTo("#111");
        assertThat(result.getGradientEnd()).isEqualTo("#222");
        assertThat(result.getIsActive()).isFalse();
    }

    @Test
    void update_blankName_keepsExistingName() {
        UUID id = UUID.randomUUID();
        Station existing = Station.builder().name("Old").build();
        when(stationRepository.findById(id)).thenReturn(Optional.of(existing));
        when(stationRepository.save(any(Station.class))).thenAnswer(inv -> inv.getArgument(0));

        Station result = service.update(id, "   ", null, null, null, null, null);

        assertThat(result.getName()).isEqualTo("Old");
    }

    @Test
    void update_withGenre_resolvesGenre() {
        UUID id = UUID.randomUUID();
        UUID newGenreId = UUID.randomUUID();
        Station existing = Station.builder().name("Old").build();
        Genre newGenre = Genre.builder().name("Pop").build();
        when(stationRepository.findById(id)).thenReturn(Optional.of(existing));
        when(genreRepository.findById(newGenreId)).thenReturn(Optional.of(newGenre));
        when(stationRepository.save(any(Station.class))).thenAnswer(inv -> inv.getArgument(0));

        Station result = service.update(id, null, newGenreId, null, null, null, null);

        assertThat(result.getGenre()).isSameAs(newGenre);
    }

    @Test
    void update_unknownGenre_throwsIllegalArgument() {
        UUID id = UUID.randomUUID();
        UUID newGenreId = UUID.randomUUID();
        Station existing = Station.builder().name("Old").build();
        when(stationRepository.findById(id)).thenReturn(Optional.of(existing));
        when(genreRepository.findById(newGenreId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, null, newGenreId, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Genre not found");
    }

    @Test
    void update_withSongIds_resolvesSongs() {
        UUID id = UUID.randomUUID();
        UUID s1 = UUID.randomUUID();
        UUID s2 = UUID.randomUUID();
        Set<UUID> songIds = Set.of(s1, s2);
        Station existing = Station.builder().name("Old").build();
        when(stationRepository.findById(id)).thenReturn(Optional.of(existing));
        when(songRepository.findAllById(anyIterable()))
                .thenReturn(List.of(Song.builder().build(), Song.builder().build()));
        when(stationRepository.save(any(Station.class))).thenAnswer(inv -> inv.getArgument(0));

        Station result = service.update(id, null, null, null, null, null, songIds);

        assertThat(result.getSongs()).hasSize(2);
    }

    @Test
    void update_withLessThanTwoSongs_throwsIllegalArgument() {
        UUID id = UUID.randomUUID();
        UUID s1 = UUID.randomUUID();
        Station existing = Station.builder().name("Old").build();
        when(stationRepository.findById(id)).thenReturn(Optional.of(existing));
        when(songRepository.findAllById(anyIterable()))
                .thenReturn(List.of(Song.builder().build()));

        assertThatThrownBy(() -> service.update(id, null, null, null, null, null, Set.of(s1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 2 songs");
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_delegatesToRepository() {
        UUID id = UUID.randomUUID();

        service.delete(id);

        verify(stationRepository).deleteById(id);
    }

    // ── resolveSongs (private path via create with empty set) ─────────────────

    @Test
    void create_emptySongSet_throwsIllegalArgument_belowMinimum() {
        UUID genreId = UUID.randomUUID();
        Genre genre = Genre.builder().name("Rock").build();
        when(stationRepository.existsByNameIgnoreCase("Rock")).thenReturn(false);
        when(genreRepository.findById(genreId)).thenReturn(Optional.of(genre));

        assertThatThrownBy(() -> service.create("Rock", genreId, "#000", "#FFF", Set.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 2 songs");

        verify(stationRepository, never()).save(any());
    }
}
