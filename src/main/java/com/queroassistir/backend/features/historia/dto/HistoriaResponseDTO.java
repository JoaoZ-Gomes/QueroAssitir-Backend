package com.queroassistir.backend.features.historia.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoriaResponseDTO {
    private String id;
    private String query;
    private String mood;
    private String context;
    private String duration;
    private HistoriaMovieDTO movie;
    private Long timestamp;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoriaMovieDTO {
        private String id;
        private String title;
        private String image;
    }
}
