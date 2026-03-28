package com.rubymusic.catalog.mapper;

import com.rubymusic.catalog.dto.AlbumResponse;
import com.rubymusic.catalog.model.Album;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = ArtistMapper.class)
public interface AlbumMapper {

    @Mapping(source = "station.id", target = "stationId")
    @Mapping(source = "station.name", target = "stationName")
    @Mapping(source = "songCount", target = "songCount")
    AlbumResponse toDto(Album album);

    List<AlbumResponse> toDtoList(List<Album> albums);
}
