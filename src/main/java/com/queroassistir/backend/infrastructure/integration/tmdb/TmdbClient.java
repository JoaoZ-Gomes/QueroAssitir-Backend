package com.queroassistir.backend.infrastructure.integration.tmdb;

import java.util.Optional;

import com.queroassistir.backend.features.filme.dto.MovieResponseDTO;

public interface TmdbClient {
    
    /**
     * Busca os detalhes de um filme no TMDB pelo ID.
     * @param movieId O ID do filme no TMDB.
     * @return Os dados do filme formatados para o DTO de resposta.
     */
    Optional<MovieResponseDTO> getMovieDetails(String movieId);
    
    Optional<MovieResponseDTO> searchMovieByTitle(String title);
}
