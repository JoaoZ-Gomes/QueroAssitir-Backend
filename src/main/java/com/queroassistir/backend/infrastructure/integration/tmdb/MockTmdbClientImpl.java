package com.queroassistir.backend.infrastructure.integration.tmdb;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.queroassistir.backend.features.filme.dto.MovieResponseDTO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MockTmdbClientImpl implements TmdbClient {

    @Override
    public Optional<MovieResponseDTO> getMovieDetails(String movieId) {
        log.info("Buscando detalhes do filme ID={} no TMDB simulado", movieId);
        
        if ("tmdb-mock-1".equals(movieId)) {
            return Optional.of(MovieResponseDTO.builder()
                    .id(movieId)
                    .title("O Guardião dos Sonhos (Original)")
                    .description("Uma aventura visual deslumbrante sobre esperança e imaginação.")
                    .image("https://images.unsplash.com/photo-1768063748336-2864cd51fb26")
                    .rating(8.5)
                    .genres(List.of("Fantasia", "Aventura"))
                    .duration("1h 52min")
                    .year(2022)
                    .director("Valentina Cortes")
                    .build());
        }
        
        return Optional.empty();
    }

    @Override
    public Optional<MovieResponseDTO> searchMovieByTitle(String title) {
        log.info("Simulando busca por título: {}", title);
        return getMovieDetails("tmdb-mock-1");
    }
}
