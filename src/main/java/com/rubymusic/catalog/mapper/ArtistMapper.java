package com.rubymusic.catalog.mapper;

import com.rubymusic.catalog.dto.ArtistResponse;
import com.rubymusic.catalog.model.Artist;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ArtistMapper {

    ArtistResponse toDto(Artist artist);

    List<ArtistResponse> toDtoList(List<Artist> artists);
}
