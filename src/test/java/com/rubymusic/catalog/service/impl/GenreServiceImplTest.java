package com.rubymusic.catalog.service.impl;

import com.rubymusic.catalog.model.Genre;
import com.rubymusic.catalog.repository.GenreRepository;
import com.rubymusic.catalog.repository.StationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenreServiceImplTest {

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private StationRepository stationRepository;

    @InjectMocks
    private GenreServiceImpl service;

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    void findAll_blankQuery_returnsAll() {
        Genre g1 = Genre.builder().name("Rock").build();
        when(genreRepository.findAll()).thenReturn(List.of(g1));

        List<Genre> result = service.findAll(null);

        assertThat(result).hasSize(1);
        verify(genreRepository).findAll();
        verify(genreRepository, never()).findByNameContainingIgnoreCaseOrderByNameAsc(any());
    }

    @Test
    void findAll_withQuery_filtersByName() {
        when(genreRepository.findByNameContainingIgnoreCaseOrderByNameAsc("rock"))
                .thenReturn(List.of(Genre.builder().name("Rock").build()));

        List<Genre> result = service.findAll("rock");

        assertThat(result).hasSize(1);
        verify(genreRepository).findByNameContainingIgnoreCaseOrderByNameAsc("rock");
    }

    @Test
    void findAll_emptyQueryString_returnsAll() {
        when(genreRepository.findAll()).thenReturn(List.of());

        List<Genre> result = service.findAll("   ");

        assertThat(result).isEmpty();
        verify(genreRepository).findAll();
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    void findById_existing_returnsGenre() {
        UUID id = UUID.randomUUID();
        Genre g = Genre.builder().name("Rock").build();
        when(genreRepository.findById(id)).thenReturn(Optional.of(g));

        Genre result = service.findById(id);

        assertThat(result).isSameAs(g);
    }

    @Test
    void findById_notFound_throwsIllegalArgument() {
        UUID id = UUID.randomUUID();
        when(genreRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Genre not found");
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_uniqueName_persists() {
        when(genreRepository.existsByNameIgnoreCase("Pop")).thenReturn(false);
        when(genreRepository.save(any(Genre.class))).thenAnswer(inv -> inv.getArgument(0));

        Genre result = service.create("Pop", "#FF0000", "#00FF00");

        assertThat(result.getName()).isEqualTo("Pop");
        assertThat(result.getGradientStart()).isEqualTo("#FF0000");
        assertThat(result.getGradientEnd()).isEqualTo("#00FF00");
        verify(genreRepository).save(any(Genre.class));
    }

    @Test
    void create_duplicateName_throwsIllegalArgument() {
        when(genreRepository.existsByNameIgnoreCase("Rock")).thenReturn(true);

        assertThatThrownBy(() -> service.create("Rock", "#000", "#FFF"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Genre already exists");

        verify(genreRepository, never()).save(any());
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void update_validFields_updatesAndSaves() {
        UUID id = UUID.randomUUID();
        Genre existing = Genre.builder().name("Old").gradientStart("#000").gradientEnd("#FFF").build();
        when(genreRepository.findById(id)).thenReturn(Optional.of(existing));
        when(genreRepository.save(any(Genre.class))).thenAnswer(inv -> inv.getArgument(0));

        Genre result = service.update(id, "New", "#111", "#222");

        assertThat(result.getName()).isEqualTo("New");
        assertThat(result.getGradientStart()).isEqualTo("#111");
        assertThat(result.getGradientEnd()).isEqualTo("#222");
    }

    @Test
    void update_nullFields_keepsExistingValues() {
        UUID id = UUID.randomUUID();
        Genre existing = Genre.builder().name("Old").gradientStart("#000").gradientEnd("#FFF").build();
        when(genreRepository.findById(id)).thenReturn(Optional.of(existing));
        when(genreRepository.save(any(Genre.class))).thenAnswer(inv -> inv.getArgument(0));

        Genre result = service.update(id, null, null, null);

        assertThat(result.getName()).isEqualTo("Old");
        assertThat(result.getGradientStart()).isEqualTo("#000");
        assertThat(result.getGradientEnd()).isEqualTo("#FFF");
    }

    @Test
    void update_blankName_keepsExistingName() {
        UUID id = UUID.randomUUID();
        Genre existing = Genre.builder().name("Old").build();
        when(genreRepository.findById(id)).thenReturn(Optional.of(existing));
        when(genreRepository.save(any(Genre.class))).thenAnswer(inv -> inv.getArgument(0));

        Genre result = service.update(id, "   ", null, null);

        assertThat(result.getName()).isEqualTo("Old");
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_unusedGenre_deletes() {
        UUID id = UUID.randomUUID();
        when(stationRepository.existsByGenreId(id)).thenReturn(false);

        service.delete(id);

        verify(genreRepository).deleteById(id);
    }

    @Test
    void delete_genreInUse_throwsIllegalArgument() {
        UUID id = UUID.randomUUID();
        when(stationRepository.existsByGenreId(id)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot delete genre");

        verify(genreRepository, never()).deleteById(any());
    }
}
