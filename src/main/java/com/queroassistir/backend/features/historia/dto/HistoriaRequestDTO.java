package com.queroassistir.backend.features.historia.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoriaRequestDTO {
    private String query;
    private String mood;
    private String context;
    private String duration;
    private String filmeId;
    private String filmeTitulo;
    private String filmeImagem;
}
