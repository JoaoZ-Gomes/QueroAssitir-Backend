package com.queroassistir.backend.infrastructure.integration.tmdb;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String baseUrl;

    public RealTmdbClientImpl(
            RestTemplate restTemplate,
            @Value("${spring.tmdb.api-key}") String apiKey,
            @Value("${spring.tmdb.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    @Override
    public Optional<MovieResponseDTO> getMovieDetails(String movieId) {
        Optional<MovieResponseDTO> movie = getMovieDetailsWithUrl(UriComponentsBuilder.fromUriString(baseUrl)
                .path("/movie/{movie_id}")
                .queryParam("api_key", apiKey)
                .queryParam("language", "pt-BR")
                .queryParam("append_to_response", "credits,watch/providers") // Busca tudo de uma vez
                .buildAndExpand(movieId)
                .toUriString(), movieId);
        
        return movie.isPresent() ? movie : generateFallbackMovie(movieId);
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

            if (response != null && !response.getResults().isEmpty()) {
                return getMovieDetails(String.valueOf(response.getResults().get(0).getId()));
            }
        } catch (Exception e) {
            log.error("Erro ao buscar filme por título: {}. Erro: {}", title, e.getMessage());
        }
        return Optional.empty();
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

    private MovieResponseDTO mapToDto(TmdbMovieResponse tmdbMovie) {
        List<String> genres = tmdbMovie.getGenres().stream()
                .map(TmdbGenre::getName)
                .collect(Collectors.toList());

        String yearStr = tmdbMovie.getReleaseDate() != null && tmdbMovie.getReleaseDate().length() >= 4
                ? tmdbMovie.getReleaseDate().substring(0, 4)
                : "Desconhecido";
        
        int year = 0;
        try { year = Integer.parseInt(yearStr); } catch (Exception ignored) {}

        String imageUrl = tmdbMovie.getPosterPath() != null
                ? "https://image.tmdb.org/t/p/w500" + tmdbMovie.getPosterPath()
                : "https://via.placeholder.com/500x750?text=Sem+Imagem";

        int durationMin = tmdbMovie.getRuntime() != null ? tmdbMovie.getRuntime() : 0;
        String durationStr = (durationMin / 60) + "h " + (durationMin % 60) + "min";

        // Diretor
        String director = "Desconhecido";
        if (tmdbMovie.getCredits() != null && tmdbMovie.getCredits().getCrew() != null) {
            director = tmdbMovie.getCredits().getCrew().stream()
                    .filter(member -> "Director".equals(member.getJob()))
                    .map(TmdbCrew::getName)
                    .findFirst()
                    .orElse("Desconhecido");
        }

        // Plataformas (Watch Providers)
        List<String> platforms = new ArrayList<>();
        if (tmdbMovie.getWatchProviders() != null && 
            tmdbMovie.getWatchProviders().getResults() != null && 
            tmdbMovie.getWatchProviders().getResults().get("BR") != null) {
            
            TmdbCountryProviders br = tmdbMovie.getWatchProviders().getResults().get("BR");
            if (br.getFlatrate() != null) {
                platforms = br.getFlatrate().stream()
                        .map(TmdbProvider::getProviderName)
                        .collect(Collectors.toList());
            }
        }

        return MovieResponseDTO.builder()
                .id(String.valueOf(tmdbMovie.getId()))
                .title(tmdbMovie.getTitle())
                .description(tmdbMovie.getOverview())
                .image(imageUrl)
                .rating(tmdbMovie.getVoteAverage())
                .genres(genres)
                .durationMinutes(durationMin)
                .duration(durationStr)
                .year(year)
                .director(director)
                .platforms(platforms)
                .build();
    }

    private Optional<MovieResponseDTO> generateFallbackMovie(String id) {
        return Optional.of(MovieResponseDTO.builder()
                .id(id)
                .title("Filme")
                .description("Dados temporariamente indisponíveis no TMDB.")
                .image("https://via.placeholder.com/500x750?text=Indisponivel")
                .rating(0.0)
                .genres(List.of())
                .platforms(List.of())
                .build());
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TmdbSearchResponse {
        private List<TmdbSearchResult> results;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TmdbSearchResult {
        private Long id;
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
        @JsonProperty("release_date")
        private String releaseDate;
        private Integer runtime;
        private List<TmdbGenre> genres;
        private TmdbCredits credits;
        @JsonProperty("watch/providers")
        private TmdbWatchProviders watchProviders;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TmdbWatchProviders {
        private java.util.Map<String, TmdbCountryProviders> results;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TmdbCountryProviders {
        private List<TmdbProvider> flatrate;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TmdbProvider {
        @JsonProperty("provider_name")
        private String providerName;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TmdbCredits {
        private List<TmdbCrew> crew;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TmdbCrew {
        private String name;
        private String job;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TmdbGenre {
        private String name;
    }
}
