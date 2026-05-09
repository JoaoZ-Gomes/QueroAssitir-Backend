package com.queroassistir.backend.infrastructure.integration.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@Primary
public class GeminiAiServiceImpl implements AiService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private static final Pattern JSON_PATTERN = Pattern.compile("\\{.*\\}", Pattern.DOTALL);

    // Dicas de estilo aleatórias para evitar que a IA sempre recomende os mesmos filmes
    private static final String[] STYLE_HINTS = {
        "Priorize filmes lançados entre 2015 e 2025.",
        "Priorize filmes clássicos (antes de 2000).",
        "Priorize filmes independentes e pouco conhecidos.",
        "Priorize filmes de diretores premiados no Oscar.",
        "Priorize filmes de cinematografias diversas (coreano, francês, espanhol, etc).",
        "Priorize filmes com nota acima de 7.5 no TMDB.",
        "Priorize filmes lançados nos últimos 3 anos.",
        "Priorize filmes cult e de nicho.",
        "Priorize filmes com elenco de grande talento.",
        "Misture filmes clássicos com lançamentos recentes.",
    };

    private static final String[] DIVERSITY_HINTS = {
        "Escolha filmes de gêneros DIFERENTES entre si para as alternativas.",
        "Cada filme alternativo deve ser de uma década diferente.",
        "Inclua pelo menos um filme não-americano nas alternativas.",
        "Surpreenda o usuário com uma escolha inesperada.",
        "Pense em filmes que poucos recomendam mas que são excelentes.",
    };

    public GeminiAiServiceImpl(org.springframework.ai.google.genai.GoogleGenAiChatModel chatModel, ObjectMapper objectMapper, 
                               @Value("${spring.ai.google.genai.api-key:}") String apiKey) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.objectMapper = objectMapper;
        
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("${GEMINI_API_KEY}")) {
            log.error("[AI-CONFIG] CRÍTICO: API Key do Gemini não foi encontrada ou está vazia!");
        } else {
            log.info("[AI-CONFIG] API Key carregada com sucesso (prefixo: {}...)", apiKey.substring(0, Math.min(apiKey.length(), 5)));
        }
    }

    @Override
    public AiRecommendationResponse getRecommendation(String mood, String context, String query, List<String> excludedMovieIds) {
        long startTime = System.currentTimeMillis();
        log.info("[AI-DEBUG] Iniciando solicitação. Mood: {}, Contexto: {}, Busca: '{}', Excluídos: {}", 
                mood, context, query, excludedMovieIds != null ? excludedMovieIds.size() : 0);

        // Selecionar dicas aleatórias para variedade
        String styleHint = STYLE_HINTS[ThreadLocalRandom.current().nextInt(STYLE_HINTS.length)];
        String diversityHint = DIVERSITY_HINTS[ThreadLocalRandom.current().nextInt(DIVERSITY_HINTS.length)];

        // Construir bloco de exclusão
        String exclusionBlock = "";
        if (excludedMovieIds != null && !excludedMovieIds.isEmpty()) {
            exclusionBlock = String.format("""
                    
                    FILMES PROIBIDOS (NÃO RECOMENDE ESTES - já foram vistos):
                    IDs TMDB a EVITAR: %s
                    Escolha filmes COMPLETAMENTE DIFERENTES dos listados acima.
                    """, String.join(", ", excludedMovieIds));
        }

        String prompt = String.format("""
                Você é um curador de cinema especialista com amplo conhecimento da base TMDB.
                
                OBJETIVO:
                Recomendar 1 filme principal e 4 alternativas para:
                - Humor: %s
                - Contexto: %s
                - Busca do Usuário: "%s"
                
                DIRETRIZ DE ESTILO (SIGA OBRIGATORIAMENTE):
                %s
                %s
                %s
                
                REGRAS DE PRIORIDADE:
                1. Se o humor for 'indiferente', FOQUE 100%% na 'Busca do Usuário'.
                2. Se houver humor E busca, equilibre ambos, priorizando gêneros mencionados na busca.
                3. NUNCA repita filmes entre principal e alternativas.
                4. Cada recomendação deve ser um filme DIFERENTE e ÚNICO.
                
                REGRAS CRÍTICAS DE ID (TMDB):
                1. Use APENAS IDs NUMÉRICOS reais do TMDB.
                2. Verifique duas vezes: o ID deve corresponder ao filme que você está recomendando.
                3. No campo 'movieTitle', coloque o título EXATO do filme para conferência.
                4. Inclua filmes que tenham POSTER disponível no TMDB (poster_path não nulo).
                5. PRIORIZE filmes de alta qualidade: nota (vote_average) >= 6.5 e com boa base de votos no TMDB.
                
                FORMATO DE RESPOSTA (JSON APENAS, sem markdown):
                {
                  "primaryMovieId": "ID_NUMERICO",
                  "movieTitle": "Nome do Filme",
                  "alternativeMovieIds": ["ID1", "ID2", "ID3", "ID4"],
                  "matchReason": "Justificativa criativa e personalizada em Português (2-3 frases)"
                }
                """, 
                mood, 
                context, 
                query != null ? query : "Nenhuma busca específica",
                styleHint,
                diversityHint,
                exclusionBlock);

        String response = null;
        try {
            log.info("[AI-DEBUG] Enviando requisição para o Google Gemini API...");
            response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            if (response == null || response.isBlank()) {
                log.warn("[AI-DEBUG] Alerta: O Gemini retornou uma resposta vazia.");
            }

            log.info("[AI-DEBUG] Conexão com IA realizada com sucesso em {}ms", (System.currentTimeMillis() - startTime));
            log.debug("[AI-DEBUG] Payload Bruto: {}", response);

            String jsonCleaned = extractJson(response);
            AiRecommendationResponse result = objectMapper.readValue(jsonCleaned, AiRecommendationResponse.class);
            
            log.info("[AI-LOG] Filme Escolhido: {} (ID: {})", result.movieTitle(), result.primaryMovieId());
            log.info("[AI-LOG] Alternativas: {} IDs", result.alternativeMovieIds().size());
            log.info("[AI-LOG] Justificativa: {}", result.matchReason());
            
            return result;

        } catch (Exception e) {
            log.error("[AI-DEBUG] ERRO NA CHAMADA AO GEMINI: {}", e.getMessage());
            // Fallback com filmes variados para não repetir sempre o mesmo
            return getRandomFallback();
        }
    }

    /**
     * Retorna um fallback aleatório em vez de sempre retornar Inception.
     */
    private AiRecommendationResponse getRandomFallback() {
        // Pool de filmes populares para fallback variado
        String[][] fallbackPool = {
            {"27205", "A Origem"},           // Inception
            {"157336", "Interestelar"},       // Interstellar
            {"550", "Clube da Luta"},         // Fight Club
            {"680", "Pulp Fiction"},          // Pulp Fiction
            {"238", "O Poderoso Chefão"},     // The Godfather
            {"278", "Um Sonho de Liberdade"}, // Shawshank Redemption
            {"155", "Batman: O Cavaleiro das Trevas"}, // Dark Knight
            {"13", "Forrest Gump"},           // Forrest Gump
            {"11", "Star Wars IV"},           // Star Wars
            {"122", "O Senhor dos Anéis: O Retorno do Rei"}, // LOTR
        };

        int idx = ThreadLocalRandom.current().nextInt(fallbackPool.length);
        String[] primary = fallbackPool[idx];

        // Selecionar 3 alternativas diferentes
        List<String> altIds = new java.util.ArrayList<>();
        java.util.Set<Integer> usedIndices = new java.util.HashSet<>();
        usedIndices.add(idx);
        while (altIds.size() < 3 && usedIndices.size() < fallbackPool.length) {
            int altIdx = ThreadLocalRandom.current().nextInt(fallbackPool.length);
            if (usedIndices.add(altIdx)) {
                altIds.add(fallbackPool[altIdx][0]);
            }
        }

        return new AiRecommendationResponse(
            primary[0], altIds,
            "Tivemos um problema técnico, mas selecionamos este clássico especialmente para você!",
            primary[1]
        );
    }

    private String extractJson(String input) {
        if (input == null) return "{}";
        Matcher matcher = JSON_PATTERN.matcher(input);
        return matcher.find() ? matcher.group() : input.trim();
    }
}
