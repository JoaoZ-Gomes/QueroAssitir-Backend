package com.queroassistir.backend.infrastructure.integration.tmdb;

import java.util.List;
import java.util.Optional;

import com.queroassistir.backend.features.filme.dto.MovieResponseDTO;

public interface TmdbClient {
    
    /**
     * Busca os detalhes de um filme no TMDB pelo ID.
     */
    Optional<MovieResponseDTO> getMovieDetails(String movieId);
    
    /**
     * Busca um filme no TMDB pelo título.
     */
    Optional<MovieResponseDTO> searchMovieByTitle(String title);

    /**
     * Descobre filmes por gênero usando paginação aleatória.
     * Combina resultados de discover, popular e top_rated.
     */
    List<MovieResponseDTO> discoverMovies(List<String> genreIds, List<String> excludeIds);

    /**
     * Busca filmes em tendência (trending).
     */
    List<MovieResponseDTO> getTrendingMovies(List<String> excludeIds);
}

