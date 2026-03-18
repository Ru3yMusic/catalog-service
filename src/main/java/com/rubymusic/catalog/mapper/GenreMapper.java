package com.rubymusic.catalog.mapper;

import com.rubymusic.catalog.dto.GenreRequest;
import com.rubymusic.catalog.dto.GenreResponse;
import com.rubymusic.catalog.model.Genre;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GenreMapper {

    GenreResponse toDto(Genre genre);

    List<GenreResponse> toDtoList(List<Genre> genres);
}
