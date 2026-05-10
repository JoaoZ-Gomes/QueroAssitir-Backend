package com.queroassistir.backend.features.recomendacao.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.queroassistir.backend.features.filme.dto.MovieResponseDTO;
import com.queroassistir.backend.features.recomendacao.dto.RecomendacaoRequestDTO;
import com.queroassistir.backend.features.recomendacao.dto.RecomendacaoResponseDTO;
import com.queroassistir.backend.infrastructure.integration.ai.AiService;
import com.queroassistir.backend.infrastructure.integration.ai.ExplanationGeneratorService;
import com.queroassistir.backend.infrastructure.integration.tmdb.TmdbClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecomendacaoService implements RecomendacaoIService {

    private final AiService aiService;
    private final TmdbClient tmdbClient;
    private final ExplanationGeneratorService explanationGenerator;

    /**
     * Mapeamento de mood → gêneros TMDB para discovery.
     * IDs: https://developer.themoviedb.org/reference/genre-movie-list
     */
    private static final Map<String, List<String>> MOOD_GENRE_MAP = Map.ofEntries(
        Map.entry("leve",       List.of("35", "10751", "16")),     // Comédia, Família, Animação
        Map.entry("emocional",  List.of("18", "10749")),            // Drama, Romance
        Map.entry("intenso",    List.of("28", "53", "80")),         // Ação, Thriller, Crime
        Map.entry("divertido",  List.of("35", "12", "16")),         // Comédia, Aventura, Animação
        Map.entry("nostalgico", List.of("18", "10749", "10402")),   // Drama, Romance, Música
        Map.entry("tenso",      List.of("53", "27", "9648")),       // Thriller, Terror, Mistério
        Map.entry("inspirado",  List.of("18", "36", "99")),         // Drama, História, Documentário
        Map.entry("caotico",    List.of("28", "878", "14")),        // Ação, Ficção Científica, Fantasia
        Map.entry("indiferente", List.of("28", "35", "18", "878"))  // Mix popular
    );

    @Override
    public RecomendacaoResponseDTO gerarRecomendacao(RecomendacaoRequestDTO request) {
        log.info("=== INICIANDO RECOMENDAÇÃO ===");
        log.info("Mood: {}, Context: {}, Query: '{}', Excluídos: {}", 
                request.getMood(), request.getContext(), request.getQuery(),
                request.getExcludedMovieIds() != null ? request.getExcludedMovieIds().size() : 0);

        List<String> excludedIds = request.getExcludedMovieIds() != null 
                ? request.getExcludedMovieIds() 
                : List.of();

        // ===== FASE 1: Obter recomendação da IA =====
        MovieResponseDTO primaryMovie = null;
        List<MovieResponseDTO> aiAlternatives = new ArrayList<>();
        String matchReason = "Selecionado especialmente para o seu momento.";

        try {
            var aiResponse = aiService.getRecommendation(
                    request.getMood(), request.getContext(), request.getQuery(), excludedIds);
            log.info("[AI] Filme primário ID: {}, Título: {}", aiResponse.primaryMovieId(), aiResponse.movieTitle());
            matchReason = aiResponse.matchReason();

            // Buscar filme principal
            primaryMovie = tmdbClient.getMovieDetails(aiResponse.primaryMovieId())
                    .filter(m -> !m.getTitle().equals("Filme"))
                    .filter(m -> !excludedIds.contains(m.getId()))
                    .orElse(null);

            // Se não encontrou por ID, tenta por título
            if (primaryMovie == null && aiResponse.movieTitle() != null) {
                log.warn("[AI] ID {} falhou. Buscando por título: {}", aiResponse.primaryMovieId(), aiResponse.movieTitle());
                primaryMovie = tmdbClient.searchMovieByTitle(aiResponse.movieTitle())
                        .filter(m -> !excludedIds.contains(m.getId()))
                        .orElse(null);
            }

            // Buscar alternativas da IA
            if (aiResponse.alternativeMovieIds() != null) {
                for (String altId : aiResponse.alternativeMovieIds()) {
                    if (excludedIds.contains(altId)) continue;
                    tmdbClient.getMovieDetails(altId)
                            .filter(m -> !m.getTitle().equals("Filme"))
                            .ifPresent(aiAlternatives::add);
                }
            }
            log.info("[AI] Alternativas encontradas: {}", aiAlternatives.size());

        } catch (Exception e) {
            log.error("[AI] Erro ao obter recomendação da IA: {}", e.getMessage());
        }

        // ===== FASE 2: Suplementar com TMDB Discovery =====
        List<MovieResponseDTO> discoveryMovies = new ArrayList<>();
        try {
            String mood = request.getMood() != null ? request.getMood().toLowerCase() : "indiferente";
            List<String> genreIds = MOOD_GENRE_MAP.getOrDefault(mood, MOOD_GENRE_MAP.get("indiferente"));
            
            log.info("[TMDB] Buscando discovery para gêneros: {}", genreIds);
            discoveryMovies.addAll(tmdbClient.discoverMovies(genreIds, excludedIds));
            
            log.info("[TMDB] Buscando trending...");
            discoveryMovies.addAll(tmdbClient.getTrendingMovies(excludedIds));
            
            log.info("[TMDB] Total discovery/trending: {} filmes", discoveryMovies.size());
        } catch (Exception e) {
            log.warn("[TMDB] Erro ao buscar discovery: {}", e.getMessage());
        }

        // ===== FASE 3: Montar resultado final =====
        // Se a IA não encontrou filme principal, usar melhor do discovery
        if (primaryMovie == null && !discoveryMovies.isEmpty()) {
            log.info("[FALLBACK] Usando filme do discovery como principal");
            primaryMovie = discoveryMovies.remove(0);
            
            if (primaryMovie.getDurationMinutes() == null || primaryMovie.getDurationMinutes() == 0) {
                primaryMovie = tmdbClient.getMovieDetails(primaryMovie.getId()).orElse(primaryMovie);
            }
            
            matchReason = "Selecionado com base no seu humor e nas tendências atuais.";
        }

        // Se AINDA não tem filme principal, erro
        if (primaryMovie == null) {
            throw new RuntimeException("Não foi possível encontrar nenhum filme para recomendar. Tente novamente.");
        }

        // ===== FASE 4: Gerar explicação personalizada =====
        // Gerar uma explicação humanizada e contextualizada para a recomendação
        try {
            matchReason = explanationGenerator.generateMatchReason(
                    primaryMovie,
                    request.getMood(),
                    request.getContext(),
                    request.getDuration(),
                    request.getQuery()
            );
            log.info("[EXPLANATION] Gerada com sucesso: {}", matchReason);
        } catch (Exception e) {
            log.warn("[EXPLANATION] Erro ao gerar explicação personalizada, usando fallback: {}", e.getMessage());
            // matchReason já possui um fallback básico
        }

        // Combinar alternativas: AI + Discovery, sem duplicatas
        Set<String> usedIds = new HashSet<>();
        usedIds.add(primaryMovie.getId());
        usedIds.addAll(excludedIds);

        List<MovieResponseDTO> finalAlternatives = new ArrayList<>();

        // Primeiro, alternativas da IA
        for (MovieResponseDTO alt : aiAlternatives) {
            if (usedIds.add(alt.getId())) {
                finalAlternatives.add(alt);
            }
        }

        // Depois, complementar com discovery (shuffled)
        Collections.shuffle(discoveryMovies);
        for (MovieResponseDTO disc : discoveryMovies) {
            if (finalAlternatives.size() >= 5) break; // máximo 5 alternativas
            if (usedIds.add(disc.getId())) {
                finalAlternatives.add(disc);
            }
        }

        // Shuffle final nas alternativas para garantir variedade
        Collections.shuffle(finalAlternatives);

        // Garantir que todos os filmes finais tenham duração (filmes do discovery vêm sem runtime)
        List<MovieResponseDTO> fullyPopulatedAlternatives = new ArrayList<>();
        for (MovieResponseDTO alt : finalAlternatives) {
            if (alt.getDurationMinutes() == null || alt.getDurationMinutes() == 0) {
                tmdbClient.getMovieDetails(alt.getId()).ifPresentOrElse(
                    fullyPopulatedAlternatives::add,
                    () -> fullyPopulatedAlternatives.add(alt)
                );
            } else {
                fullyPopulatedAlternatives.add(alt);
            }
        }
        finalAlternatives = fullyPopulatedAlternatives;

        // 3. Montar a resposta final
        if (matchReason == null || matchReason.isBlank()) {
            matchReason = "Selecionado especialmente para o seu momento.";
        }

        log.info("=== RECOMENDAÇÃO CONCLUÍDA ===");
        log.info("Principal: {} ({})", primaryMovie.getTitle(), primaryMovie.getId());
        log.info("Alternativas: {}", finalAlternatives.stream()
                .map(m -> m.getTitle() + " (" + m.getId() + ")")
                .collect(Collectors.joining(", ")));

        return RecomendacaoResponseDTO.builder()
                .primary(primaryMovie)
                .alternatives(finalAlternatives)
                .matchReason(matchReason)
                .mood(request.getMood())
                .query(request.getQuery())
                .context(request.getContext())
                .build();
    }
}
