package com.queroassistir.backend.infrastructure.integration.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@Primary
public class GeminiAiServiceImpl implements AiService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private static final Pattern JSON_PATTERN = Pattern.compile("\\{.*\\}", Pattern.DOTALL);

    public GeminiAiServiceImpl(org.springframework.ai.google.genai.GoogleGenAiChatModel chatModel, ObjectMapper objectMapper) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.objectMapper = objectMapper;
    }

    @Override
    public AiRecommendationResponse getRecommendation(String mood, String context, String query) {
        long startTime = System.currentTimeMillis();
        log.info("[AI-DEBUG] Iniciando solicitação. Mood: {}, Contexto: {}, Busca: '{}'", mood, context, query);

        String prompt = String.format("""
                Você é um especialista em cinema e base de dados do TMDB.
                
                OBJETIVO:
                Recomendar 1 filme principal e 3 alternativas para:
                - Humor: %s
                - Contexto: %s
                - Busca do Usuário: "%s"
                
                REGRAS DE PRIORIDADE (MUITO IMPORTANTE):
                1. Se o humor for 'indiferente', FOQUE 100%% na 'Busca do Usuário' para decidir o gênero e o tom do filme.
                2. Se houver humor e busca, tente equilibrar os dois, mas priorize termos de gênero (ex: Terror, Sci-fi) que apareçam na busca.
                
                REGRAS CRÍTICAS DE ID (TMDB):
                1. Você DEVE fornecer o ID NUMÉRICO real do TMDB. 
                2. Verifique duas vezes o ID: 
                   - Drácula (1931) é 348 (NÃO use 120, 120 é Senhor dos Anéis).
                   - Frankenstein (1931) é 3035.
                   - Psico (1960) é 213.
                3. No campo 'movieTitle', coloque o título do filme que você escolheu para conferência.
                
                FORMATO DE RESPOSTA (JSON APENAS):
                {
                  "primaryMovieId": "ID_NUMERICO",
                  "movieTitle": "Nome do Filme",
                  "alternativeMovieIds": ["ID1", "ID2", "ID3"],
                  "matchReason": "Justificativa criativa em Português (2 frases)"
                }
                """, mood, context, query != null ? query : "Nenhuma busca específica");

        String response = null;
        try {
            response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            log.info("[AI-DEBUG] Resposta recebida em {}ms", (System.currentTimeMillis() - startTime));
            log.debug("[AI-DEBUG] Payload Bruto: {}", response);

            String jsonCleaned = extractJson(response);
            AiRecommendationResponse result = objectMapper.readValue(jsonCleaned, AiRecommendationResponse.class);
            
            log.info("[AI-LOG] Filme Escolhido: {} (ID: {})", result.movieTitle(), result.primaryMovieId());
            log.info("[AI-LOG] Justificativa: {}", result.matchReason());
            
            return result;

        } catch (Exception e) {
            log.error("[AI-DEBUG] ERRO NA CHAMADA AO GEMINI: {}", e.getMessage());
            return new AiRecommendationResponse("27205", List.of("157336", "155"), 
                "Tivemos um problema técnico, mas este filme é uma ótima escolha!", "Inception");
        }
    }

    private String extractJson(String input) {
        if (input == null) return "{}";
        Matcher matcher = JSON_PATTERN.matcher(input);
        return matcher.find() ? matcher.group() : input.trim();
    }
}
