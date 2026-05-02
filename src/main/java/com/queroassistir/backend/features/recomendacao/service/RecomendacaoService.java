package com.queroassistir.backend.features.recomendacao.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.queroassistir.backend.features.filme.dto.MovieResponseDTO;
import com.queroassistir.backend.features.recomendacao.dto.RecomendacaoRequestDTO;
import com.queroassistir.backend.features.recomendacao.dto.RecomendacaoResponseDTO;
import com.queroassistir.backend.infrastructure.integration.ai.AiService;
import com.queroassistir.backend.infrastructure.integration.tmdb.TmdbClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecomendacaoService implements RecomendacaoIService {

    private final AiService aiService;
    private final TmdbClient tmdbClient;

    @Override
    public RecomendacaoResponseDTO gerarRecomendacao(RecomendacaoRequestDTO request) {
        log.info("Iniciando processo de recomendação para mood={} e context={}", request.getMood(), request.getContext());
        
        // 1. Chamar a IA para decidir os filmes
        var aiResponse = aiService.getRecommendation(request.getMood(), request.getContext(), request.getQuery());
        log.debug("IA recomendou o filme primário ID: {}", aiResponse.primaryMovieId());

        // 2. Buscar os detalhes técnicos do filme no TMDB
        MovieResponseDTO primaryMovie = tmdbClient.getMovieDetails(aiResponse.primaryMovieId())
                .filter(m -> !m.getTitle().equals("Filme")) // Filtra se for o nosso fallback genérico
                .orElseGet(() -> {
                    log.warn("ID {} falhou. Tentando busca por título: {}", aiResponse.primaryMovieId(), aiResponse.movieTitle());
                    return tmdbClient.searchMovieByTitle(aiResponse.movieTitle())
                            .orElseThrow(() -> new RuntimeException("Falha total ao encontrar o filme " + aiResponse.movieTitle()));
                });

        // 2.1 Buscar alternativas (opcional, se falhar apenas ignora)
        List<MovieResponseDTO> alternatives = aiResponse.alternativeMovieIds().stream()
                .map(id -> {
                    Optional<MovieResponseDTO> m = tmdbClient.getMovieDetails(id);
                    return m.filter(movie -> !movie.getTitle().equals("Filme"));
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        // 3. Montar a resposta final
        log.info("Recomendação concluída com sucesso. Filme selecionado: {}. Alternativas encontradas: {}", 
                primaryMovie.getTitle(), alternatives.size());
        
        return RecomendacaoResponseDTO.builder()
                .primary(primaryMovie)
                .alternatives(alternatives)
                .matchReason(aiResponse.matchReason())
                .mood(request.getMood())
                .query(request.getQuery())
                .context(request.getContext())
                .build();
    }
}

