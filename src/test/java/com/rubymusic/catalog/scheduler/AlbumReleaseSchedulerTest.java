package com.rubymusic.catalog.scheduler;

import com.rubymusic.catalog.event.MusicFeedEventPublisher;
import com.rubymusic.catalog.model.Album;
import com.rubymusic.catalog.model.Artist;
import com.rubymusic.catalog.repository.AlbumRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlbumReleaseSchedulerTest {

    @Mock private AlbumRepository albumRepository;
    @Mock private MusicFeedEventPublisher musicFeedEventPublisher;

    @InjectMocks
    private AlbumReleaseScheduler scheduler;

    @Test
    void publishDueAlbums_noAlbumsDue_doesNothing() {
        when(albumRepository.findAllByReleasedFalseAndReleaseDateTimeLessThanEqual(any()))
                .thenReturn(List.of());

        scheduler.publishDueAlbums();

        verify(albumRepository, never()).saveAll(any());
        verifyNoInteractions(musicFeedEventPublisher);
    }

    @Test
    void publishDueAlbums_marksReleased_savesAndPublishes() {
        Artist artist = Artist.builder()
                .id(UUID.randomUUID())
                .name("Test Artist")
                .build();
        Album a1 = Album.builder()
                .id(UUID.randomUUID())
                .title("Album 1")
                .artist(artist)
                .released(false)
                .releaseDateTime(LocalDateTime.now().minusMinutes(5))
                .build();
        Album a2 = Album.builder()
                .id(UUID.randomUUID())
                .title("Album 2")
                .artist(artist)
                .released(false)
                .releaseDateTime(LocalDateTime.now().minusMinutes(1))
                .build();

        when(albumRepository.findAllByReleasedFalseAndReleaseDateTimeLessThanEqual(any()))
                .thenReturn(List.of(a1, a2));

        scheduler.publishDueAlbums();

        assertThat(a1.getReleased()).isTrue();
        assertThat(a2.getReleased()).isTrue();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Album>> captor = ArgumentCaptor.forClass(List.class);
        verify(albumRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).containsExactly(a1, a2);

        verify(musicFeedEventPublisher).publishAlbumReleased(a1);
        verify(musicFeedEventPublisher).publishAlbumReleased(a2);
        verify(musicFeedEventPublisher, times(2)).publishAlbumReleased(any(Album.class));
    }
}
