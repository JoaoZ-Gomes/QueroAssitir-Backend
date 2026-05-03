package com.queroassistir.backend.infrastructure.integration.ai;

import java.util.List;

public interface AiService {
    
    /**
     * Pede à IA para gerar uma recomendação de filme baseada no humor e contexto.
     * @param mood O humor do usuário.
     * @param context O contexto do usuário.
     * @param query A busca do usuário.
     * @param excludedMovieIds IDs de filmes a evitar (já recomendados anteriormente).
     * @return O ID do filme recomendado (ex: do TMDB) e a justificativa.
     */
    AiRecommendationResponse getRecommendation(String mood, String context, String query, List<String> excludedMovieIds);

    record AiRecommendationResponse(String primaryMovieId, List<String> alternativeMovieIds, String matchReason, String movieTitle) {}
}
