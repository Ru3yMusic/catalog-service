package com.rubymusic.catalog.service.impl;

import com.rubymusic.catalog.event.MusicFeedEventPublisher;
import com.rubymusic.catalog.model.Album;
import com.rubymusic.catalog.model.Artist;
import com.rubymusic.catalog.model.Station;
import com.rubymusic.catalog.repository.AlbumRepository;
import com.rubymusic.catalog.repository.ArtistRepository;
import com.rubymusic.catalog.repository.SongRepository;
import com.rubymusic.catalog.repository.StationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlbumServiceImplTest {

    @Mock
    private AlbumRepository albumRepository;

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private StationRepository stationRepository;

    @Mock
    private SongRepository songRepository;

    @Mock
    private MusicFeedEventPublisher musicFeedEventPublisher;

    @InjectMocks
    private AlbumServiceImpl service;

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    void findAll_withArtistId_filtersByArtist() {
        UUID artistId = UUID.randomUUID();
        Pageable pageable = Pageable.ofSize(10);
        when(albumRepository.findByArtistId(artistId, pageable)).thenReturn(Page.empty());

        service.findAll(artistId, pageable);

        verify(albumRepository).findByArtistId(artistId, pageable);
        verify(albumRepository, never()).findAll(pageable);
    }

    @Test
    void findAll_nullArtistId_returnsAll() {
        Pageable pageable = Pageable.ofSize(10);
        when(albumRepository.findAll(pageable)).thenReturn(Page.empty());

        service.findAll(null, pageable);

        verify(albumRepository).findAll(pageable);
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    void findById_existing_returnsAlbum() {
        UUID id = UUID.randomUUID();
        Album a = Album.builder().title("Dark Side").build();
        when(albumRepository.findById(id)).thenReturn(Optional.of(a));

        Album result = service.findById(id);

        assertThat(result).isSameAs(a);
    }

    @Test
    void findById_notFound_throwsIllegalArgument() {
        UUID id = UUID.randomUUID();
        when(albumRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Album not found");
    }

    // ── delegations ───────────────────────────────────────────────────────────

    @Test
    void findByArtistId_delegatesToRepository() {
        UUID artistId = UUID.randomUUID();
        Pageable pageable = Pageable.ofSize(10);
        when(albumRepository.findByArtistId(artistId, pageable)).thenReturn(Page.empty());

        service.findByArtistId(artistId, pageable);

        verify(albumRepository).findByArtistId(artistId, pageable);
    }

    @Test
    void findNewReleases_delegatesToRepository() {
        Pageable pageable = Pageable.ofSize(10);
        when(albumRepository.findAllByReleasedTrueOrderByReleaseDateTimeDesc(pageable))
                .thenReturn(Page.empty());

        service.findNewReleases(pageable);

        verify(albumRepository).findAllByReleasedTrueOrderByReleaseDateTimeDesc(pageable);
    }

    @Test
    void findTopByStreams_delegatesToRepository() {
        Pageable pageable = Pageable.ofSize(10);
        when(albumRepository.findAllByOrderByTotalStreamsDesc(pageable)).thenReturn(Page.empty());

        service.findTopByStreams(pageable);

        verify(albumRepository).findAllByOrderByTotalStreamsDesc(pageable);
    }

    @Test
    void search_delegatesToRepository() {
        Pageable pageable = Pageable.ofSize(10);
        when(albumRepository.findByTitleContainingIgnoreCaseOrderByReleaseDateTimeDesc("dark", pageable))
                .thenReturn(Page.empty());

        service.search("dark", pageable);

        verify(albumRepository).findByTitleContainingIgnoreCaseOrderByReleaseDateTimeDesc("dark", pageable);
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_pastReleaseDate_publishesEvent() {
        UUID artistId = UUID.randomUUID();
        Artist artist = Artist.builder().name("Floyd").build();
        when(artistRepository.findById(artistId)).thenReturn(Optional.of(artist));
        when(albumRepository.save(any(Album.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime past = LocalDateTime.now().minusDays(1);
        Album result = service.create("Dark Side", artistId, "cover.jpg", past, null);

        assertThat(result.getReleased()).isTrue();
        assertThat(result.getStation()).isNull();
        verify(musicFeedEventPublisher).publishAlbumReleased(result);
    }

    @Test
    void create_futureReleaseDate_doesNotPublishEvent() {
        UUID artistId = UUID.randomUUID();
        Artist artist = Artist.builder().name("Floyd").build();
        when(artistRepository.findById(artistId)).thenReturn(Optional.of(artist));
        when(albumRepository.save(any(Album.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime future = LocalDateTime.now().plusDays(7);
        Album result = service.create("Future Release", artistId, "cover.jpg", future, null);

        assertThat(result.getReleased()).isFalse();
        verify(musicFeedEventPublisher, never()).publishAlbumReleased(any());
    }

    @Test
    void create_withStation_resolvesStation() {
        UUID artistId = UUID.randomUUID();
        UUID stationId = UUID.randomUUID();
        Artist artist = Artist.builder().name("Floyd").build();
        Station station = Station.builder().name("Rock Classics").build();
        when(artistRepository.findById(artistId)).thenReturn(Optional.of(artist));
        when(stationRepository.findById(stationId)).thenReturn(Optional.of(station));
        when(albumRepository.save(any(Album.class))).thenAnswer(inv -> inv.getArgument(0));

        Album result = service.create("Dark Side", artistId, "cover.jpg",
                LocalDateTime.now().minusDays(1), stationId);

        assertThat(result.getStation()).isSameAs(station);
    }

    @Test
    void create_unknownArtist_throwsIllegalArgument() {
        UUID artistId = UUID.randomUUID();
        when(artistRepository.findById(artistId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create("X", artistId, null,
                LocalDateTime.now(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Artist not found");
    }

    @Test
    void create_unknownStation_throwsIllegalArgument() {
        UUID artistId = UUID.randomUUID();
        UUID stationId = UUID.randomUUID();
        when(artistRepository.findById(artistId)).thenReturn(Optional.of(Artist.builder().build()));
        when(stationRepository.findById(stationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create("X", artistId, null,
                LocalDateTime.now(), stationId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Station not found");
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void update_releaseDateMovedToPast_publishesEvent() {
        UUID id = UUID.randomUUID();
        Album existing = Album.builder()
                .title("Old")
                .releaseDateTime(LocalDateTime.now().plusDays(7))
                .released(false)
                .build();
        when(albumRepository.findById(id)).thenReturn(Optional.of(existing));
        when(albumRepository.save(any(Album.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime past = LocalDateTime.now().minusDays(1);
        Album result = service.update(id, "New", "cover.jpg", past, null);

        assertThat(result.getReleased()).isTrue();
        verify(musicFeedEventPublisher).publishAlbumReleased(result);
    }

    @Test
    void update_alreadyReleased_doesNotRepublish() {
        UUID id = UUID.randomUUID();
        Album existing = Album.builder()
                .title("Old")
                .releaseDateTime(LocalDateTime.now().minusDays(2))
                .released(true)
                .build();
        when(albumRepository.findById(id)).thenReturn(Optional.of(existing));
        when(albumRepository.save(any(Album.class))).thenAnswer(inv -> inv.getArgument(0));

        service.update(id, "Updated", null, null, null);

        verify(musicFeedEventPublisher, never()).publishAlbumReleased(any());
    }

    @Test
    void update_releaseDateMovedToFuture_albumHidden() {
        UUID id = UUID.randomUUID();
        Album existing = Album.builder()
                .title("Old")
                .releaseDateTime(LocalDateTime.now().minusDays(2))
                .released(true)
                .build();
        when(albumRepository.findById(id)).thenReturn(Optional.of(existing));
        when(albumRepository.save(any(Album.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime future = LocalDateTime.now().plusDays(7);
        Album result = service.update(id, null, null, future, null);

        assertThat(result.getReleased()).isFalse();
        verify(musicFeedEventPublisher, never()).publishAlbumReleased(any());
    }

    @Test
    void update_withStation_resolvesStation() {
        UUID id = UUID.randomUUID();
        UUID stationId = UUID.randomUUID();
        Album existing = Album.builder()
                .title("Old")
                .releaseDateTime(LocalDateTime.now().minusDays(2))
                .released(true)
                .build();
        Station station = Station.builder().name("Rock").build();
        when(albumRepository.findById(id)).thenReturn(Optional.of(existing));
        when(stationRepository.findById(stationId)).thenReturn(Optional.of(station));
        when(albumRepository.save(any(Album.class))).thenAnswer(inv -> inv.getArgument(0));

        Album result = service.update(id, null, null, null, stationId);

        assertThat(result.getStation()).isSameAs(station);
    }

    @Test
    void update_blankTitle_keepsExisting() {
        UUID id = UUID.randomUUID();
        Album existing = Album.builder()
                .title("Old")
                .releaseDateTime(LocalDateTime.now().minusDays(2))
                .released(true)
                .build();
        when(albumRepository.findById(id)).thenReturn(Optional.of(existing));
        when(albumRepository.save(any(Album.class))).thenAnswer(inv -> inv.getArgument(0));

        Album result = service.update(id, "   ", null, null, null);

        assertThat(result.getTitle()).isEqualTo("Old");
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_removesSongsFromStations_thenDeletes() {
        UUID id = UUID.randomUUID();

        service.delete(id);

        verify(songRepository).removeAlbumSongsFromAllStations(id);
        verify(albumRepository).deleteById(id);
    }
}
