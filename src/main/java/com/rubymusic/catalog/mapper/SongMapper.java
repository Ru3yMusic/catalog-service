package com.rubymusic.catalog.mapper;

import com.rubymusic.catalog.dto.SongResponse;
import com.rubymusic.catalog.model.Song;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = {ArtistMapper.class, AlbumMapper.class, GenreMapper.class})
public interface SongMapper {

    SongResponse toDto(Song song);

    List<SongResponse> toDtoList(List<Song> songs);
}
