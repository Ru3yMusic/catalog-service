package com.rubymusic.catalog.mapper;

import com.rubymusic.catalog.dto.StationResponse;
import com.rubymusic.catalog.model.Station;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StationMapper {

    @Mapping(source = "genre.id", target = "genreId")
    @Mapping(source = "genre.name", target = "genreName")
    @Mapping(source = "songCount", target = "songCount")
    StationResponse toDto(Station station);

    List<StationResponse> toDtoList(List<Station> stations);
}
