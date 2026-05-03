package com.queroassistir.backend.infrastructure.integration.tmdb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.queroassistir.backend.features.filme.dto.MovieResponseDTO;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Primary
public class RealTmdbClientImpl implements TmdbClient {

    private static final String FALLBACK_IMAGE = "https://placehold.co/500x750/1a1a2e/c4b5fd?text=Sem+Poster";
    private static final String TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p/w500";

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String baseUrl;

    // Configurações de Qualidade
    private final double minVoteAverage;
    private final int minVoteCount;

    public RealTmdbClientImpl(
            RestTemplate restTemplate,
            @Value("${spring.tmdb.api-key}") String apiKey,
            @Value("${spring.tmdb.base-url}") String baseUrl,
            @Value("${recommendation.quality.min-vote-average:6.0}") double minVoteAverage,
            @Value("${recommendation.quality.min-vote-count:100}") int minVoteCount) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.minVoteAverage = minVoteAverage;
        this.minVoteCount = minVoteCount;
    }

    // ===================== MÉTODOS EXISTENTES =====================

    @Override
    public Optional<MovieResponseDTO> getMovieDetails(String movieId) {
        return getMovieDetailsWithUrl(UriComponentsBuilder.fromUriString(baseUrl)
                .path("/movie/{movie_id}")
                .queryParam("api_key", apiKey)
                .queryParam("language", "pt-BR")
                .queryParam("append_to_response", "credits,watch/providers")
                .buildAndExpand(movieId)
                .toUriString(), movieId);
    }

    @Override
    public Optional<MovieResponseDTO> searchMovieByTitle(String title) {
        try {
            String url = UriComponentsBuilder.fromUriString(baseUrl)
                    .path("/search/movie")
                    .queryParam("api_key", apiKey)
                    .queryParam("query", title)
                    .queryParam("language", "pt-BR")
                    .build()
                    .toUriString();

            TmdbSearchResponse response = restTemplate.getForObject(url, TmdbSearchResponse.class);

            if (response != null && response.getResults() != null && !response.getResults().isEmpty()) {
                // Pegar o primeiro resultado que atenda a critérios mínimos de qualidade, se possível
                List<TmdbMovieResponse> sortedResults = response.getResults().stream()
                        .sorted((a, b) -> Double.compare(
                                b.getVoteAverage() != null ? b.getVoteAverage() : 0, 
                                a.getVoteAverage() != null ? a.getVoteAverage() : 0))
                        .collect(Collectors.toList());

                for (TmdbMovieResponse result : sortedResults) {
                    Optional<MovieResponseDTO> movie = getMovieDetails(String.valueOf(result.getId()));
                    if (movie.isPresent() && !movie.get().getTitle().equals("Filme")) {
                        return movie;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Erro ao buscar filme por título: {}. Erro: {}", title, e.getMessage());
        }
        return Optional.empty();
    }

    // ===================== NOVOS MÉTODOS DE DISCOVERY =====================

    @Override
    public List<MovieResponseDTO> discoverMovies(List<String> genreIds, List<String> excludeIds) {
        log.info("Descobrindo filmes de qualidade. Gêneros: {}", genreIds);
        
        List<MovieResponseDTO> pool = new ArrayList<>();

        // 1. Discover por gênero (várias páginas para ter de onde filtrar)
        if (genreIds != null && !genreIds.isEmpty()) {
            String genres = String.join(",", genreIds);
            pool.addAll(fetchFromEndpoint("/discover/movie", genres, 1));
            pool.addAll(fetchFromEndpoint("/discover/movie", genres, 2));
        }

        // 2. Popular e Top Rated (fontes de qualidade garantida)
        pool.addAll(fetchPopularOrTopRated("/movie/popular", 1));
        pool.addAll(fetchPopularOrTopRated("/movie/top_rated", 1));

        // 3. Processar Qualidade e Ranking
        return processQualityAndRanking(pool, excludeIds, 10);
    }

    @Override
    public List<MovieResponseDTO> getTrendingMovies(List<String> excludeIds) {
        log.info("Buscando filmes em tendência de alta qualidade");
        
        List<MovieResponseDTO> pool = new ArrayList<>();
        try {
            String timeWindow = ThreadLocalRandom.current().nextBoolean() ? "day" : "week";
            String url = UriComponentsBuilder.fromUriString(baseUrl)
                    .path("/trending/movie/{time_window}")
                    .queryParam("api_key", apiKey)
                    .queryParam("language", "pt-BR")
                    .buildAndExpand(timeWindow)
                    .toUriString();

            TmdbListResponse response = restTemplate.getForObject(url, TmdbListResponse.class);
            if (response != null && response.getResults() != null) {
                pool = response.getResults().stream()
                        .map(this::mapToDtoSafe)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("Erro ao buscar trending: {}", e.getMessage());
        }

        return processQualityAndRanking(pool, excludeIds, 5);
    }

    // ===================== LÓGICA DE QUALIDADE E RANKING =====================

    private List<MovieResponseDTO> processQualityAndRanking(List<MovieResponseDTO> pool, List<String> excludeIds, int limit) {
        List<String> excluded = excludeIds != null ? excludeIds : List.of();

        // Etapa 1: Filtro de Qualidade Rígido
        List<MovieResponseDTO> filtered = pool.stream()
                .filter(m -> !excluded.contains(m.getId()))
                .filter(m -> !m.getTitle().equals("Filme"))
                .filter(m -> m.getRating() >= minVoteAverage)
                .filter(m -> m.getVoteCount() >= minVoteCount)
                .filter(m -> m.getImage() != null && !m.getImage().contains("placehold"))
                .collect(Collectors.toList());

        // Etapa 2: Relaxamento de filtro se a lista for muito pequena
        if (filtered.size() < limit) {
            log.info("Poucos resultados de alta qualidade ({}/{}). Relaxando filtros...", filtered.size(), limit);
            List<MovieResponseDTO> relaxed = pool.stream()
                    .filter(m -> !excluded.contains(m.getId()))
                    .filter(m -> !filtered.contains(m)) // evita duplicatas
                    .filter(m -> m.getRating() >= (minVoteAverage - 1.0)) // relaxa nota
                    .filter(m -> m.getVoteCount() >= (minVoteCount / 2)) // relaxa votos
                    .collect(Collectors.toList());
            filtered.addAll(relaxed);
        }

        // Etapa 3: Cálculo de Score e Ordenação
        // Score = (Nota * 0.6) + (log10(votos) * 0.3) + (Random * 0.1)
        return filtered.stream()
                .sorted(Comparator.comparingDouble(this::calculateScore).reversed())
                .limit(limit * 2) // Pega o dobro do limite para embaralhar o topo
                .collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
                    Collections.shuffle(list);
                    return list.stream().limit(limit).collect(Collectors.toList());
                }));
    }

    private double calculateScore(MovieResponseDTO m) {
        double ratingPart = m.getRating() * 0.6;
        double popularityPart = Math.log10(Math.max(1, m.getVoteCount())) * 0.3;
        double randomPart = ThreadLocalRandom.current().nextDouble(0, 1) * 0.1;
        return ratingPart + popularityPart + randomPart;
    }

    // ===================== MÉTODOS PRIVADOS DE BUSCA =====================

    private List<MovieResponseDTO> fetchFromEndpoint(String path, String genreIds, int page) {
        try {
            String url = UriComponentsBuilder.fromUriString(baseUrl)
                    .path(path)
                    .queryParam("api_key", apiKey)
                    .queryParam("language", "pt-BR")
                    .queryParam("sort_by", "popularity.desc")
                    .queryParam("with_genres", genreIds)
                    .queryParam("page", page)
                    .queryParam("vote_count.gte", 50) // Pré-filtro na API para performance
                    .build()
                    .toUriString();

            TmdbListResponse response = restTemplate.getForObject(url, TmdbListResponse.class);
            if (response != null && response.getResults() != null) {
                return response.getResults().stream()
                        .map(this::mapToDtoSafe)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("Erro ao buscar filmes de {}: {}", path, e.getMessage());
        }
        return List.of();
    }

    private List<MovieResponseDTO> fetchPopularOrTopRated(String path, int page) {
        try {
            String url = UriComponentsBuilder.fromUriString(baseUrl)
                    .path(path)
                    .queryParam("api_key", apiKey)
                    .queryParam("language", "pt-BR")
                    .queryParam("page", page)
                    .build()
                    .toUriString();

            TmdbListResponse response = restTemplate.getForObject(url, TmdbListResponse.class);
            if (response != null && response.getResults() != null) {
                return response.getResults().stream()
                        .map(this::mapToDtoSafe)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("Erro ao buscar filmes de {}: {}", path, e.getMessage());
        }
        return List.of();
    }

    private Optional<MovieResponseDTO> getMovieDetailsWithUrl(String url, String originalId) {
        try {
            TmdbMovieResponse response = restTemplate.getForObject(url, TmdbMovieResponse.class);
            if (response != null) {
                return Optional.of(mapToDto(response));
            }
        } catch (Exception e) {
            log.warn("Falha ao buscar detalhes para ID: {}. URL: {}", originalId, url);
        }
        return Optional.empty();
    }

    private MovieResponseDTO mapToDtoSafe(TmdbMovieResponse tmdbMovie) {
        try {
            return mapToDto(tmdbMovie);
        } catch (Exception e) {
            return null;
        }
    }

    private MovieResponseDTO mapToDto(TmdbMovieResponse tmdbMovie) {
        List<String> genres = new ArrayList<>();
        if (tmdbMovie.getGenres() != null) {
            genres = tmdbMovie.getGenres().stream().map(TmdbGenre::getName).collect(Collectors.toList());
        } else if (tmdbMovie.getGenreIds() != null) {
            genres = tmdbMovie.getGenreIds().stream().map(this::getGenreName).filter(Objects::nonNull).collect(Collectors.toList());
        }

        String yearStr = tmdbMovie.getReleaseDate() != null && tmdbMovie.getReleaseDate().length() >= 4
                ? tmdbMovie.getReleaseDate().substring(0, 4) : "0";
        int year = 0;
        try { year = Integer.parseInt(yearStr); } catch (Exception ignored) {}

        String imageUrl = buildImageUrl(tmdbMovie.getPosterPath());
        int durationMin = tmdbMovie.getRuntime() != null ? tmdbMovie.getRuntime() : 0;
        String durationStr = durationMin > 0 ? (durationMin / 60) + "h " + (durationMin % 60) + "min" : "Duração indisponível";

        String director = "Desconhecido";
        if (tmdbMovie.getCredits() != null && tmdbMovie.getCredits().getCrew() != null) {
            director = tmdbMovie.getCredits().getCrew().stream()
                    .filter(member -> "Director".equals(member.getJob()))
                    .map(TmdbCrew::getName)
                    .findFirst().orElse("Desconhecido");
        }

        List<String> platforms = new ArrayList<>();
        if (tmdbMovie.getWatchProviders() != null && tmdbMovie.getWatchProviders().getResults() != null 
            && tmdbMovie.getWatchProviders().getResults().get("BR") != null) {
            TmdbCountryProviders br = tmdbMovie.getWatchProviders().getResults().get("BR");
            if (br.getFlatrate() != null) {
                platforms = br.getFlatrate().stream().map(TmdbProvider::getProviderName).collect(Collectors.toList());
            }
        }

        return MovieResponseDTO.builder()
                .id(String.valueOf(tmdbMovie.getId()))
                .title(tmdbMovie.getTitle())
                .description(tmdbMovie.getOverview())
                .image(imageUrl)
                .rating(tmdbMovie.getVoteAverage() != null ? tmdbMovie.getVoteAverage() : 0.0)
                .voteCount(tmdbMovie.getVoteCount() != null ? tmdbMovie.getVoteCount() : 0)
                .genres(genres)
                .durationMinutes(durationMin)
                .duration(durationStr)
                .year(year)
                .director(director)
                .platforms(platforms)
                .build();
    }

    private String buildImageUrl(String posterPath) {
        if (posterPath != null && !posterPath.isBlank()) {
            return posterPath.startsWith("http") ? posterPath : TMDB_IMAGE_BASE + posterPath;
        }
        return FALLBACK_IMAGE;
    }

    private String getGenreName(Integer genreId) {
        return switch (genreId) {
            case 28 -> "Ação"; case 12 -> "Aventura"; case 16 -> "Animação"; case 35 -> "Comédia";
            case 80 -> "Crime"; case 99 -> "Documentário"; case 18 -> "Drama"; case 10751 -> "Família";
            case 14 -> "Fantasia"; case 36 -> "História"; case 27 -> "Terror"; case 10402 -> "Música";
            case 9648 -> "Mistério"; case 10749 -> "Romance"; case 878 -> "Ficção Científica";
            case 10770 -> "Filme para TV"; case 53 -> "Thriller"; case 10752 -> "Guerra"; case 37 -> "Faroeste";
            default -> null;
        };
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TmdbListResponse {
        private List<TmdbMovieResponse> results;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TmdbSearchResponse {
        private List<TmdbMovieResponse> results;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TmdbMovieResponse {
        private Long id;
        private String title;
        private String overview;
        @JsonProperty("poster_path")
        private String posterPath;
        @JsonProperty("vote_average")
        private Double voteAverage;
        @JsonProperty("vote_count")
        private Integer voteCount;
        @JsonProperty("release_date")
        private String releaseDate;
        private Integer runtime;
        private List<TmdbGenre> genres;
        @JsonProperty("genre_ids")
        private List<Integer> genreIds;
        private TmdbCredits credits;
        @JsonProperty("watch/providers")
        private TmdbWatchProviders watchProviders;
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true) public static class TmdbWatchProviders { private java.util.Map<String, TmdbCountryProviders> results; }
    @Data @JsonIgnoreProperties(ignoreUnknown = true) public static class TmdbCountryProviders { private List<TmdbProvider> flatrate; }
    @Data @JsonIgnoreProperties(ignoreUnknown = true) public static class TmdbProvider { @JsonProperty("provider_name") private String providerName; }
    @Data @JsonIgnoreProperties(ignoreUnknown = true) public static class TmdbCredits { private List<TmdbCrew> crew; }
    @Data @JsonIgnoreProperties(ignoreUnknown = true) public static class TmdbCrew { private String name; private String job; }
    @Data @JsonIgnoreProperties(ignoreUnknown = true) public static class TmdbGenre { private String name; }
}
