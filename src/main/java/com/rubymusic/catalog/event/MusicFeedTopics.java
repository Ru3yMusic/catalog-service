package com.rubymusic.catalog.event;

/**
 * Kafka topic names for the {@code music-feed.*} family — events that drive the
 * realtime UI feed at {@code /user/music}. Decoupled from existing topics like
 * {@code song.liked}: those carry UUID-only payloads for counter updates, while
 * music-feed events carry full JSON payloads so the WebSocket consumer can
 * broadcast a renderable card without refetching from the catalog API.
 */
public final class MusicFeedTopics {

    public static final String ALBUM_RELEASED = "music-feed.album.released";
    public static final String ARTIST_TOP_CHANGED = "music-feed.artist.top-changed";

    private MusicFeedTopics() {}
}
