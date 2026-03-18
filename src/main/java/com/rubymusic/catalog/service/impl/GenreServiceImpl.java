package com.rubymusic.catalog.service.impl;

import com.rubymusic.catalog.model.Genre;
import com.rubymusic.catalog.repository.GenreRepository;
import com.rubymusic.catalog.service.GenreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GenreServiceImpl implements GenreService {

    private final GenreRepository genreRepository;

    @Override
    public List<Genre> findAll() {
        return genreRepository.findAll();
    }

    @Override
    public Genre findById(UUID id) {
        return genreRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Genre not found: " + id));
    }

    @Override
    @Transactional
    public Genre create(String name, String gradientStart, String gradientEnd) {
        if (genreRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Genre already exists: " + name);
        }
        Genre genre = Genre.builder()
                .name(name)
                .gradientStart(gradientStart)
                .gradientEnd(gradientEnd)
                .build();
        return genreRepository.save(genre);
    }

    @Override
    @Transactional
    public Genre update(UUID id, String name, String gradientStart, String gradientEnd) {
        Genre genre = findById(id);
        if (name != null && !name.isBlank()) genre.setName(name);
        if (gradientStart != null) genre.setGradientStart(gradientStart);
        if (gradientEnd != null) genre.setGradientEnd(gradientEnd);
        return genreRepository.save(genre);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        genreRepository.deleteById(id);
    }
}
