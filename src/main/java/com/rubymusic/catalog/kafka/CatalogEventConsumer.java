package com.rubymusic.catalog.kafka;

import com.rubymusic.catalog.service.ArtistService;
import com.rubymusic.catalog.service.SongService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consumes Kafka events from interaction-service and social-service
 * to keep catalog counters eventually consistent.
 *
 * <p>Topics:
 * <ul>
 *   <li>{@code song.played}      — increments play_count</li>
 *   <li>{@code song.liked}       — increments likes_count</li>
 *   <li>{@code song.unliked}     — decrements likes_count</li>
 *   <li>{@code artist.followed}  — increments followers_count</li>
 *   <li>{@code artist.unfollowed}— decrements followers_count</li>
 * </ul>
 *
 * <p>Message format: plain UUID string (the target resource ID).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogEventConsumer {

    private final SongService songService;
    private final ArtistService artistService;

    @KafkaListener(topics = "song.played", groupId = "catalog-service")
    public void onSongPlayed(String songIdStr) {
        try {
            UUID songId = UUID.fromString(songIdStr.trim());
            songService.incrementPlayCount(songId);
            log.debug("play_count incremented for song {}", songId);
        } catch (Exception e) {
            log.error("Failed to process song.played event [{}]: {}", songIdStr, e.getMessage());
        }
    }

    @KafkaListener(topics = "song.liked", groupId = "catalog-service")
    public void onSongLiked(String songIdStr) {
        try {
            UUID songId = UUID.fromString(songIdStr.trim());
            songService.incrementLikesCount(songId);
            log.debug("likes_count incremented for song {}", songId);
        } catch (Exception e) {
            log.error("Failed to process song.liked event [{}]: {}", songIdStr, e.getMessage());
        }
    }

    @KafkaListener(topics = "song.unliked", groupId = "catalog-service")
    public void onSongUnliked(String songIdStr) {
        try {
            UUID songId = UUID.fromString(songIdStr.trim());
            songService.decrementLikesCount(songId);
            log.debug("likes_count decremented for song {}", songId);
        } catch (Exception e) {
            log.error("Failed to process song.unliked event [{}]: {}", songIdStr, e.getMessage());
        }
    }

    @KafkaListener(topics = "artist.followed", groupId = "catalog-service")
    public void onArtistFollowed(String artistIdStr) {
        try {
            UUID artistId = UUID.fromString(artistIdStr.trim());
            artistService.incrementFollowersCount(artistId);
            log.debug("followers_count incremented for artist {}", artistId);
        } catch (Exception e) {
            log.error("Failed to process artist.followed event [{}]: {}", artistIdStr, e.getMessage());
        }
    }

    @KafkaListener(topics = "artist.unfollowed", groupId = "catalog-service")
    public void onArtistUnfollowed(String artistIdStr) {
        try {
            UUID artistId = UUID.fromString(artistIdStr.trim());
            artistService.decrementFollowersCount(artistId);
            log.debug("followers_count decremented for artist {}", artistId);
        } catch (Exception e) {
            log.error("Failed to process artist.unfollowed event [{}]: {}", artistIdStr, e.getMessage());
        }
    }
}
