package com.queroassistir.backend.infrastructure.integration.ai;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j
@Service
public class MockAiServiceImpl implements AiService {

    @Override
    public AiRecommendationResponse getRecommendation(String mood, String context, String query) {
        log.info("Gerando recomendação MOCK para mood={} e context={}", mood, context);
        // Simulando delay de rede
        try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        
        // Retorna "A Origem" (Inception) como mock
        return new AiRecommendationResponse(
            "27205", 
            List.of("157336", "155", "27205"), 
            "Este filme foi selecionado via MOCK para teste do fluxo.",
            "A Origem"
        );
    }
}
