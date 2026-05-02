package com.rubymusic.catalog.service.impl;

import com.rubymusic.catalog.event.MusicFeedEventPublisher;
import com.rubymusic.catalog.model.Artist;
import com.rubymusic.catalog.repository.ArtistRepository;
import com.rubymusic.catalog.repository.SongRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

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
class ArtistServiceImplTest {

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private SongRepository songRepository;

    @Mock
    private MusicFeedEventPublisher musicFeedEventPublisher;

    @InjectMocks
    private ArtistServiceImpl service;

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    void findAll_isTopTrue_filtersTopArtists() {
        Pageable pageable = Pageable.ofSize(10);
        Page<Artist> page = new PageImpl<>(List.of(Artist.builder().name("Top").build()));
        when(artistRepository.findByIsTopTrueOrderByCreatedAtDesc(pageable)).thenReturn(page);

        Page<Artist> result = service.findAll(true, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(artistRepository).findByIsTopTrueOrderByCreatedAtDesc(pageable);
        verify(artistRepository, never()).findAll(pageable);
    }

    @Test
    void findAll_isTopNullOrFalse_returnsAll() {
        Pageable pageable = Pageable.ofSize(10);
        when(artistRepository.findAll(pageable)).thenReturn(Page.empty());

        service.findAll(null, pageable);
        service.findAll(false, pageable);

        verify(artistRepository, never()).findByIsTopTrueOrderByCreatedAtDesc(any());
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    void findById_existing_returnsArtist() {
        UUID id = UUID.randomUUID();
        Artist a = Artist.builder().name("Bowie").build();
        when(artistRepository.findById(id)).thenReturn(Optional.of(a));

        Artist result = service.findById(id);

        assertThat(result).isSameAs(a);
    }

    @Test
    void findById_notFound_throwsIllegalArgument() {
        UUID id = UUID.randomUUID();
        when(artistRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Artist not found");
    }

    // ── findTopArtists ────────────────────────────────────────────────────────

    @Test
    void findTopArtists_delegatesToRepository() {
        when(artistRepository.findByIsTopTrueOrderByMonthlyListenersDesc())
                .thenReturn(List.of(Artist.builder().name("Bowie").build()));

        List<Artist> result = service.findTopArtists();

        assertThat(result).hasSize(1);
    }

    // ── search ────────────────────────────────────────────────────────────────

    @Test
    void search_delegatesToRepository() {
        Pageable pageable = Pageable.ofSize(10);
        when(artistRepository.findByNameContainingIgnoreCase("bow", pageable))
                .thenReturn(Page.empty());

        service.search("bow", pageable);

        verify(artistRepository).findByNameContainingIgnoreCase("bow", pageable);
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_topArtist_publishesEvent() {
        when(artistRepository.save(any(Artist.class))).thenAnswer(inv -> {
            Artist a = inv.getArgument(0);
            a.setIsTop(true);
            return a;
        });

        Artist result = service.create("Bowie", "url", "bio", true);

        assertThat(result.getName()).isEqualTo("Bowie");
        verify(musicFeedEventPublisher).publishArtistTopChanged(result);
    }

    @Test
    void create_nonTopArtist_doesNotPublishEvent() {
        when(artistRepository.save(any(Artist.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create("Bowie", "url", "bio", false);

        verify(musicFeedEventPublisher, never()).publishArtistTopChanged(any());
    }

    @Test
    void create_isTopNull_treatsAsFalse() {
        when(artistRepository.save(any(Artist.class))).thenAnswer(inv -> inv.getArgument(0));

        Artist result = service.create("Bowie", "url", "bio", null);

        assertThat(result.getIsTop()).isFalse();
        verify(musicFeedEventPublisher, never()).publishArtistTopChanged(any());
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void update_topFlagFlipsToTrue_publishesEvent() {
        UUID id = UUID.randomUUID();
        Artist existing = Artist.builder().name("Old").isTop(false).build();
        when(artistRepository.findById(id)).thenReturn(Optional.of(existing));
        when(artistRepository.save(any(Artist.class))).thenAnswer(inv -> inv.getArgument(0));

        Artist result = service.update(id, "New", "url", "bio", true);

        assertThat(result.getName()).isEqualTo("New");
        assertThat(result.getIsTop()).isTrue();
        verify(musicFeedEventPublisher).publishArtistTopChanged(result);
    }

    @Test
    void update_topFlagFlipsToFalse_publishesEvent() {
        UUID id = UUID.randomUUID();
        Artist existing = Artist.builder().name("Old").isTop(true).build();
        when(artistRepository.findById(id)).thenReturn(Optional.of(existing));
        when(artistRepository.save(any(Artist.class))).thenAnswer(inv -> inv.getArgument(0));

        Artist result = service.update(id, null, null, null, false);

        assertThat(result.getIsTop()).isFalse();
        verify(musicFeedEventPublisher).publishArtistTopChanged(result);
    }

    @Test
    void update_topFlagSame_doesNotPublish() {
        UUID id = UUID.randomUUID();
        Artist existing = Artist.builder().name("Old").isTop(true).build();
        when(artistRepository.findById(id)).thenReturn(Optional.of(existing));
        when(artistRepository.save(any(Artist.class))).thenAnswer(inv -> inv.getArgument(0));

        service.update(id, "New", null, null, true);

        verify(musicFeedEventPublisher, never()).publishArtistTopChanged(any());
    }

    @Test
    void update_blankName_keepsExistingName() {
        UUID id = UUID.randomUUID();
        Artist existing = Artist.builder().name("Old").isTop(false).build();
        when(artistRepository.findById(id)).thenReturn(Optional.of(existing));
        when(artistRepository.save(any(Artist.class))).thenAnswer(inv -> inv.getArgument(0));

        Artist result = service.update(id, "   ", null, null, null);

        assertThat(result.getName()).isEqualTo("Old");
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_removesArtistSongsFromStations_thenDeletes() {
        UUID id = UUID.randomUUID();

        service.delete(id);

        verify(songRepository).removeArtistSongsFromAllStations(id);
        verify(artistRepository).deleteById(id);
    }

    // ── counter methods ───────────────────────────────────────────────────────

    @Test
    void incrementFollowersCount_delegatesToRepository() {
        UUID id = UUID.randomUUID();

        service.incrementFollowersCount(id);

        verify(artistRepository).incrementFollowersCount(id);
    }

    @Test
    void decrementFollowersCount_delegatesToRepository() {
        UUID id = UUID.randomUUID();

        service.decrementFollowersCount(id);

        verify(artistRepository).decrementFollowersCount(id);
    }
}
