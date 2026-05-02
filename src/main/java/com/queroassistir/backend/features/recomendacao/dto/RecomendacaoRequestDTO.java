package com.queroassistir.backend.features.recomendacao.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecomendacaoRequestDTO {

    @NotBlank(message = "O humor é obrigatório")
    private String mood;

    @NotBlank(message = "O contexto é obrigatório")
    private String context;

    private String duration;

    private String query;

    private List<String> excludedMovieIds;

    private List<String> preferredPlatforms;
}
