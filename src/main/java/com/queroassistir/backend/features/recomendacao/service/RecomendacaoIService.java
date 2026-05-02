package com.queroassistir.backend.features.recomendacao.service;

import com.queroassistir.backend.features.recomendacao.dto.RecomendacaoRequestDTO;
import com.queroassistir.backend.features.recomendacao.dto.RecomendacaoResponseDTO;

public interface RecomendacaoIService {
    RecomendacaoResponseDTO gerarRecomendacao(RecomendacaoRequestDTO request);
}
