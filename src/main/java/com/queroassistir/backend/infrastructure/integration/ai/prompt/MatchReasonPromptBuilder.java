package com.queroassistir.backend.infrastructure.integration.ai.prompt;

/**
 * Engenharia de Prompts Profissional para Explicação de Recomendações
 * 
 * OBJETIVO:
 * - Gerar explicações personalizadas e humanizadas
 * - Considerar contexto completo do usuário
 * - Evitar respostas genéricas e repetitivas
 * - Produzir textos cinematográficos e impactantes
 * 
 * ESTRATÉGIA:
 * 1. Few-shot learning com exemplos reais
 * 2. Contexto estruturado (mood, context, duration, query, filme)
 * 3. Instruções claras de tom e estilo
 * 4. Validações de qualidade
 * 5. Temperatura alta para variedade
 */
public class MatchReasonPromptBuilder {

    /**
     * Constrói o prompt SYSTEM para definir o comportamento da IA
     * 
     * Este é o contexto que define como o Gemini deve pensar e responder
     */
    public static String buildSystemPrompt() {
        return """
                Você é um crítico de cinema apaixonado e especialista em psicologia do espectador.
                Seu trabalho é explicar POR QUE um filme foi recomendado, de forma pessoal e cinematográfica.
                
                CARACTERÍSTICAS ESPERADAS:
                
                1. HUMANIDADE: Escreva como um amigo recomendando um filme, não como um bot
                   ❌ ERRADO: "Perfeito para quem gosta de suspense"
                   ✅ CERTO: "Como você quer algo tenso para assistir sozinho, esse filme funciona bem porque..."
                
                2. ESPECIFICIDADE: Mencione detalhes do contexto do usuário
                   - "Já que você busca X..." (reference a query)
                   - "Para assistir sozinho..." (reference o context)
                   - "Em um momento tenso..." (reference o mood)
                   - "Para uma sessão rápida..." (reference a duration)
                
                3. CINEMATIC LANGUAGE: Use linguagem de cinema
                   Palavras-chave: atmosfera, tensão, ritmo, narrativa, camadas, nuances, subtext, 
                   cinematografia, direção, performance, presença, intensidade, suspeita
                
                4. BREVIDADE COM IMPACTO: 2-3 frases curtas e impactantes
                   Não escreva parágrafos. Seja conciso mas memorável.
                
                5. EVITE CLICHÊS: Nunca repita frases como:
                   ❌ "Ótimo filme para esse momento"
                   ❌ "Perfeito para relaxar"
                   ❌ "Uma obra-prima"
                   ❌ "Selecionado especialmente para você"
                
                6. SURPREENDA: Mencione algo inesperado ou um detalhe que não é óbvio
                   Ex: Uma subtext oculta, um detalhe de direção, uma performance subtil
                
                REGRAS PARA A RESPOSTA:
                - Português naturalizado (brasileiro)
                - Máximo 150 caracteres (não conte: menção do filme, contexto puro)
                - Nunca repita keywords do mood/context
                - Se for "leve", não diga "para relaxar" - diga algo sobre a natureza do filme
                - Cada resposta deve ser única, mesmo para o mesmo filme com moods diferentes
                
                ESTRUTURA RECOMENDADA:
                1. Abertura conectando ao estado emocional/contexto do usuário (opcional - "Como você...")
                2. O que o filme oferece especificamente para esse estado
                3. Um detalhe cinematográfico ou de linguagem que justifique
                """;
    }

    /**
     * Constrói exemplos (few-shot learning) para guiar o Gemini
     */
    public static String buildExamples() {
        return """
                EXEMPLOS DE RECOMENDAÇÕES BEM-FEITAS:
                
                Exemplo 1:
                Filme: "Mulher Homem Aranha" | Mood: tenso | Context: sozinho | Query: horror psicológico
                RUIM: "Um filme tenso perfeito para quando você está sozinho."
                BOM: "A paranoia e isolamento visual do filme criam uma experiência sufocante que reverbera enquanto você está sozinho. Cada cena carrega uma tensão psicológica que não deixa de crescer."
                
                Exemplo 2:
                Filme: "Clueless" | Mood: divertido | Context: amigos | Query: comédia leve
                RUIM: "Uma comédia divertida para assistir com amigos."
                BOM: "O humor aguçado entre os personagens cria aquele tipo de conversa que te faz querer pausar e discutir com seus amigos a cada minuto. Perfeito para uma noite onde risadas compartilhadas são o ponto."
                
                Exemplo 3:
                Filme: "Blade Runner 2049" | Mood: intenso | Context: sozinho | Query: ficção científica
                RUIM: "Uma ficção científica intensa para momentos contemplativo."
                BOM: "A visual storytelling e silêncios calculados do filme criam uma experiência imersiva que exige sua atenção completa. Sozinho, você sente cada detalhe da melancolia futurista."
                
                Exemplo 4:
                Filme: "O Retorno" | Mood: emocional | Context: amigos | Query: drama
                RUIM: "Um drama emocional ótimo para assistir com amigos."
                BOM: "As performances subtis criam momentos de conexão genuína que vão gerar conversas profundas depois. Um filme que aproxima quem está ao seu lado."
                
                Exemplo 5:
                Filme: "Mad Max Fury Road" | Mood: caotico | Context: sozinho | Query: ação adrenalina
                RUIM: "Um filme de ação perfeito para ação e adrenalina."
                BOM: "Dois horas de caos visual e sonoro onde a narrativa é o movimento em si. Sozinho, você mergulha totalmente nesse mundo frenético sem distrações."
                
                Exemplo 6:
                Filme: "Amelie" | Mood: nostalgico | Context: amigos | Query: romance fantasia
                RUIM: "Um filme nostálgico e romântico para assistir com amigos."
                BOM: "Cada quadro parece saído de uma memória querida reimaginada. Compartilhar essa sensação de magia cotidiana com quem está ao seu lado amplifica o encanto."
                """;
    }

    /**
     * Constrói o prompt USER com contexto estruturado
     */
    public static String buildUserPrompt(
            String mood,
            String context,
            String duration,
            String query,
            String movieTitle,
            String movieGenres,
            String moviePlot) {
        
        return String.format("""
                CONTEXTO DO USUÁRIO:
                - Humor Atual: %s
                - Contexto: Assistindo %s
                - Duração Desejada: %s
                - Busca/Interesse: %s
                
                FILME RECOMENDADO:
                Título: %s
                Gêneros: %s
                Sinopse: %s
                
                TAREFA:
                Explique em 1-2 frases POR QUE este filme foi recomendado especificamente para esse usuário nesse momento.
                
                IMPORTANTE:
                1. Conecte o mood/contexto/query do usuário com qualidades específicas do filme
                2. Evite ser genérico - seja preciso e pessoal
                3. Use linguagem de cinema, não buzzwords
                4. Nunca repita as palavras do mood ou contexto
                5. Seja memorável mas breve
                
                FORMATO DE RESPOSTA OBRIGATÓRIO:
                Retorne APENAS um JSON válido. Não inclua marcação markdown ou texto adicional.
                {
                  "explanation": "Sua explicação com no máximo 150 caracteres"
                }
                """,
                getMoodLabel(mood),
                getContextLabel(context),
                getDurationLabel(duration),
                query != null && !query.isBlank() ? query : "nenhuma busca específica",
                movieTitle,
                movieGenres,
                moviePlot
        );
    }

    /**
     * Retorna label humanizado para o mood
     */
    private static String getMoodLabel(String mood) {
        return switch (mood) {
            case "leve" -> "leve e despreocupado";
            case "emocional" -> "emocional e reflexivo";
            case "intenso" -> "intenso e imersivo";
            case "divertido" -> "divertido e descontraído";
            case "nostalgico" -> "nostálgico e contemplativo";
            case "tenso" -> "tenso e aprehensivo";
            case "inspirado" -> "inspirado e esperançoso";
            case "caotico" -> "caótico e vibrante";
            default -> mood;
        };
    }

    /**
     * Retorna label humanizado para o contexto
     */
    private static String getContextLabel(String context) {
        return switch (context) {
            case "sozinho" -> "sozinho, sem distrações";
            case "amigos" -> "com amigos para compartilhar a experiência";
            default -> context;
        };
    }

    /**
     * Retorna label humanizado para a duração
     */
    private static String getDurationLabel(String duration) {
        return switch (duration) {
            case "curto" -> "curta (menos de 90 minutos)";
            case "longo" -> "longa e imersiva";
            case "qualquer" -> "qualquer duração";
            default -> duration;
        };
    }

    /**
     * Prompt alternativo para gerar múltiplas explicações diferentes
     * Usado quando queremos variar a resposta mesmo para o mesmo filme
     */
    public static String buildVariationPrompt(
            String mood,
            String context,
            String query,
            String movieTitle,
            String previousExplanation) {
        
        return String.format("""
                Você já explicou assim:
                "%s"
                
                Agora, crie uma EXPLICAÇÃO DIFERENTE para o mesmo filme com contexto diferente.
                
                Novo Contexto:
                - Mood: %s
                - Assistindo: %s
                - Busca: %s
                
                Filme: %s
                
                Gere uma nova perspectiva, um novo ângulo, uma nova razão pela qual este filme funciona.
                Use um tom ligeiramente diferente. Mencione algo que não foi mencionado antes.
                
                Máximo 150 caracteres. Sem JSON, sem aspas, sem marcadores.
                """,
                previousExplanation,
                mood,
                context,
                query,
                movieTitle
        );
    }

    /**
     * Fallback automático com qualidade profissional
     * Usado quando a IA falha ou não consegue gerar algo bom
     */
    public static String getFallbackExplanation(String mood, String context, String movieTitle, String genres) {
        // Fallbacks contextualizados e naturais
        if ("sozinho".equalsIgnoreCase(context)) {
            return switch (mood) {
                case "tenso" -> "A tensão e isolamento visual criam uma experiência sufocante que ressoa enquanto você está sozinho.";
                case "emocional" -> "Performances subtis e silêncios calculados revelam toda sua profundidade quando você tem tempo para contemplar.";
                case "caotico" -> "Um rodemoinho visual que exige sua atenção total sem as distrações do convívio.";
                case "nostalgico" -> "Cada quadro convida você a explorar suas próprias memórias enquanto as do filme se desdobram.";
                default -> "Selecionado para uma jornada solo que vai prender você do início ao fim.";
            };
        } else {
            return switch (mood) {
                case "divertido" -> "O humor aguçado entre personagens cria aquele tipo de conversa que te faz pausar e discutir a cada minuto.";
                case "emocional" -> "Uma obra que aproxima quem está ao seu lado através de genuínas conexões humanas.";
                case "tenso" -> "Suspense que cria uma experiência compartilhada onde cada surpresa intensifica a tensão do grupo.";
                default -> "Uma história que funciona melhor quando compartilhada com quem está ao seu lado.";
            };
        }
    }
}
