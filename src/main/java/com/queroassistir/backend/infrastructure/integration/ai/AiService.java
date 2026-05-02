package com.queroassistir.backend.infrastructure.integration.ai;

import java.util.List;

public interface AiService {
    
    /**
     * Pede à IA para gerar uma recomendação de filme baseada no humor e contexto.
     * @param mood O humor do usuário.
     * @param context O contexto do usuário.
     * @return O ID do filme recomendado (ex: do TMDB) e a justificativa.
     */
    AiRecommendationResponse getRecommendation(String mood, String context, String query);

    record AiRecommendationResponse(String primaryMovieId, List<String> alternativeMovieIds, String matchReason, String movieTitle) {}
}
