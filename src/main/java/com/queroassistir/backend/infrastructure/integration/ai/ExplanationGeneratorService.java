package com.queroassistir.backend.infrastructure.integration.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.queroassistir.backend.features.filme.dto.MovieResponseDTO;
import com.queroassistir.backend.infrastructure.integration.ai.prompt.MatchReasonPromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Serviço especializado em gerar explicações personalizadas e humanizadas
 * para recomendações de filmes.
 * 
 * Responsabilidades:
 * - Gerar matchReason contextualizado e único
 * - Usar engenharia de prompts profissional
 * - Logging detalhado para debug
 * - Fallback elegante
 * - Evitar respostas genéricas
 * 
 * Exemplo de saída esperada:
 * ✅ "A paranoia e isolamento visual criam uma experiência sufocante que reverbera enquanto você está sozinho."
 * ❌ "Perfeito para quando você quer algo tenso para assistir sozinho."
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExplanationGeneratorService {

    private final ChatClient chatClient;

    @Value("${app.ai.explanation.enabled:true}")
    private boolean explanationEnabled;

    @Value("${app.ai.explanation.temperature:0.8}")
    private float temperature;

    /**
     * Gera uma explicação personalizada para por que um filme foi recomendado
     * 
     * @param movie Filme recomendado
     * @param mood Humor do usuário
     * @param context Contexto (sozinho/amigos)
     * @param duration Duração desejada
     * @param query Busca/interesse do usuário
     * @return Explicação humanizada e contextualizada
     */
    public String generateMatchReason(
            MovieResponseDTO movie,
            String mood,
            String context,
            String duration,
            String query) {

        if (!explanationEnabled) {
            log.debug("[EXPLANATION] Explicações desabilitadas - usando fallback");
            return MatchReasonPromptBuilder.getFallbackExplanation(mood, context, movie.getTitle(), 
                    String.join(", ", movie.getGenres()));
        }

        long startTime = System.currentTimeMillis();
        String explanation = null;

        try {
            log.info("[EXPLANATION] Gerando explicação para: {} | Mood: {} | Context: {}", 
                    movie.getTitle(), mood, context);

            // Construir o prompt USER com contexto estruturado
            String userPrompt = MatchReasonPromptBuilder.buildUserPrompt(
                    mood,
                    context,
                    duration,
                    query,
                    movie.getTitle(),
                    String.join(", ", movie.getGenres()),
                    movie.getOverview()
            );

            // Construir o prompt SYSTEM que define o comportamento
            String systemPrompt = MatchReasonPromptBuilder.buildSystemPrompt();

            log.debug("[EXPLANATION-PROMPT] System Prompt enviado para IA");
            log.trace("[EXPLANATION-PROMPT-CONTENT] {}", systemPrompt);

            // Executar chamada para o Gemini com temperatura personalizada
            explanation = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();

            long duration_ms = System.currentTimeMillis() - startTime;

            // Log de sucesso
            if (explanation != null && !explanation.isBlank()) {
                explanation = explanation.trim().replaceAll("^[\"']|[\"']$", "");
                
                log.info("[EXPLANATION-SUCCESS] Gerada em {}ms para '{}' | Tamanho: {} chars",
                        duration_ms, movie.getTitle(), explanation.length());
                log.debug("[EXPLANATION-OUTPUT] {}", explanation);

                // Validar se a explicação não ficou genérica demais
                validateExplanation(explanation, mood, context);

                return explanation;
            } else {
                log.warn("[EXPLANATION] IA retornou resposta vazia");
                return getFallback(mood, context, movie, false);
            }

        } catch (Exception e) {
            log.error("[EXPLANATION-ERROR] Erro ao gerar explicação: {} | Message: {}", 
                    e.getClass().getSimpleName(), e.getMessage());
            log.debug("[EXPLANATION-STACKTRACE]", e);
            
            return getFallback(mood, context, movie, true);
        }
    }

    /**
     * Valida se a explicação não ficou muito genérica
     */
    private void validateExplanation(String explanation, String mood, String context) {
        // Palavras-chave que indicam resposta genérica (falta personalização)
        String[] genericKeywords = {
            "perfeito para",
            "ótimo para",
            "bom para",
            "ideal para",
            "recomendado para",
            "especialmente para você"
        };

        for (String keyword : genericKeywords) {
            if (explanation.toLowerCase().contains(keyword)) {
                log.warn("[EXPLANATION-VALIDATION] Possível resposta genérica detectada: '{}'", 
                        explanation.substring(0, Math.min(50, explanation.length())));
            }
        }

        // Validar que mencionou contexto ou especificidade
        boolean mencionouContexto = false;
        if ("sozinho".equalsIgnoreCase(context) && 
            (explanation.toLowerCase().contains("sozinho") || 
             explanation.toLowerCase().contains("distrações") ||
             explanation.toLowerCase().contains("presença"))) {
            mencionouContexto = true;
        }
        if ("amigos".equalsIgnoreCase(context) && 
            (explanation.toLowerCase().contains("amigo") || 
             explanation.toLowerCase().contains("compartilh") ||
             explanation.toLowerCase().contains("conversa"))) {
            mencionouContexto = true;
        }

        if (!mencionouContexto) {
            log.debug("[EXPLANATION-VALIDATION] Contexto não foi muito explorado na explicação");
        }
    }

    /**
     * Retorna fallback de alta qualidade baseado no contexto
     */
    private String getFallback(String mood, String context, MovieResponseDTO movie, boolean duarErro) {
        String reason = MatchReasonPromptBuilder.getFallbackExplanation(
                mood, context, movie.getTitle(), String.join(", ", movie.getGenres()));
        
        if (duarErro) {
            log.info("[EXPLANATION-FALLBACK] Usando fallback devido a erro");
        } else {
            log.info("[EXPLANATION-FALLBACK] Usando fallback genérico de qualidade");
        }
        
        return reason;
    }

    /**
     * Valida se a resposta da IA é apropriada
     * Retorna true se a qualidade é aceitável
     */
    private boolean isQualityResponse(String explanation) {
        if (explanation == null || explanation.isBlank()) {
            return false;
        }

        // Mínimo de 20 caracteres
        if (explanation.length() < 20) {
            log.warn("[EXPLANATION-QA] Resposta muito curta: {} caracteres", explanation.length());
            return false;
        }

        // Máximo de 200 caracteres (para evitar respostas muito longas)
        if (explanation.length() > 200) {
            log.warn("[EXPLANATION-QA] Resposta muito longa: {} caracteres (máximo: 200)", 
                    explanation.length());
            return false;
        }

        return true;
    }
}
