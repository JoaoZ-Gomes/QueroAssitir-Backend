package com.queroassistir.backend.features.recomendacao.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.queroassistir.backend.features.recomendacao.dto.RecomendacaoRequestDTO;
import com.queroassistir.backend.features.recomendacao.dto.RecomendacaoResponseDTO;
import com.queroassistir.backend.features.recomendacao.service.RecomendacaoIService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Tag(name = "Recomendações", description = "Endpoints para o motor de recomendação baseado em IA")
public class RecomendacaoController {

    private final RecomendacaoIService service;

    @Operation(summary = "Gera recomendações", description = "Baseado no humor e contexto, retorna uma recomendação primária e alternativas.")
    @PostMapping
    public ResponseEntity<RecomendacaoResponseDTO> recomendar(@RequestBody @Valid RecomendacaoRequestDTO dto) {
        log.info("Recebida requisição de recomendação. Mood: {}, Context: {}", dto.getMood(), dto.getContext());
        return ResponseEntity.ok(service.gerarRecomendacao(dto));
    }
}

