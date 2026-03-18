package com.rubymusic.catalog.mapper;

import com.rubymusic.catalog.dto.AlbumResponse;
import com.rubymusic.catalog.model.Album;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = ArtistMapper.class)
public interface AlbumMapper {

    AlbumResponse toDto(Album album);

    List<AlbumResponse> toDtoList(List<Album> albums);
}
