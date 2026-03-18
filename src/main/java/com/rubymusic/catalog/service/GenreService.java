package com.rubymusic.catalog.service;

import com.rubymusic.catalog.model.Genre;

import java.util.List;
import java.util.UUID;

public interface GenreService {

    List<Genre> findAll();

    Genre findById(UUID id);

    Genre create(String name, String gradientStart, String gradientEnd);

    Genre update(UUID id, String name, String gradientStart, String gradientEnd);

    void delete(UUID id);
}
