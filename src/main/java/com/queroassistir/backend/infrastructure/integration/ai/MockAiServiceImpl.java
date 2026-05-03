package com.queroassistir.backend.infrastructure.integration.ai;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class MockAiServiceImpl implements AiService {

    private static final String[][] MOCK_MOVIES = {
        {"27205", "A Origem"},
        {"157336", "Interestelar"},
        {"155", "Batman: O Cavaleiro das Trevas"},
        {"550", "Clube da Luta"},
        {"680", "Pulp Fiction"},
    };

    @Override
    public AiRecommendationResponse getRecommendation(String mood, String context, String query, List<String> excludedMovieIds) {
        log.info("Gerando recomendação MOCK para mood={} e context={}", mood, context);
        try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        
        // Escolher aleatoriamente para simular variedade
        int idx = ThreadLocalRandom.current().nextInt(MOCK_MOVIES.length);
        String[] primary = MOCK_MOVIES[idx];

        return new AiRecommendationResponse(
            primary[0], 
            List.of("157336", "155", "550"), 
            "Este filme foi selecionado via MOCK para teste do fluxo.",
            primary[1]
        );
    }
}
