package com.queroassistir.backend.features.recomendacao.dto;

import java.util.List;

import com.queroassistir.backend.features.filme.dto.MovieResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecomendacaoResponseDTO {
    private MovieResponseDTO primary;
    private List<MovieResponseDTO> alternatives;
    private String matchReason;
    private String mood;
    private String query;
    private String context;
}
