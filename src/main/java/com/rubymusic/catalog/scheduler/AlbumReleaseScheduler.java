package com.rubymusic.catalog.scheduler;

import com.rubymusic.catalog.event.MusicFeedEventPublisher;
import com.rubymusic.catalog.model.Album;
import com.rubymusic.catalog.repository.AlbumRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Polls every minute for albums whose scheduled release moment has arrived
 * and flips {@code released} to true. This is what makes admin-scheduled
 * albums (e.g. "publish tomorrow at 3:45 PM") become public automatically.
 *
 * <p>Precision is ±60 seconds, which is plenty for music release UX.
 *
 * <p>Idempotent and safe to run on multiple instances: {@code released=true}
 * rows are filtered out by the repository query, so a second pass on the
 * same album does nothing. (When we eventually scale catalog-service to N
 * replicas we'll need ShedLock or similar to avoid duplicate Kafka emits.)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlbumReleaseScheduler {

    private final AlbumRepository albumRepository;
    private final MusicFeedEventPublisher musicFeedEventPublisher;

    @Scheduled(fixedRateString = "${catalog.scheduler.album-release.rate-ms:60000}")
    @Transactional
    public void publishDueAlbums() {
        LocalDateTime now = LocalDateTime.now();
        List<Album> due = albumRepository.findAllByReleasedFalseAndReleaseDateTimeLessThanEqual(now);

        if (due.isEmpty()) {
            return;
        }

        for (Album album : due) {
            album.setReleased(Boolean.TRUE);
        }
        albumRepository.saveAll(due);

        // Spring queues these and only fires Kafka sends AFTER_COMMIT — if the
        // saveAll above rolls back, no realtime broadcast goes out.
        for (Album album : due) {
            musicFeedEventPublisher.publishAlbumReleased(album);
        }

        log.info("AlbumReleaseScheduler — published {} album(s) whose release moment passed", due.size());
    }
}
