package com.queroassistir.backend.features.historia.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.queroassistir.backend.features.historia.dto.HistoriaRequestDTO;
import com.queroassistir.backend.features.historia.dto.HistoriaResponseDTO;
import com.queroassistir.backend.features.historia.service.HistoriaService;
import com.queroassistir.backend.infrastructure.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
@Tag(name = "Histórico", description = "Endpoints para gerenciar histórico de buscas")
public class HistoriaController {

    private final HistoriaService service;

    @Operation(summary = "Salvar nova entrada de histórico", description = "Registra uma nova busca de recomendação no histórico")
    @PostMapping
    public ResponseEntity<ApiResponse<HistoriaResponseDTO>> salvar(@RequestBody HistoriaRequestDTO dto) {
        log.info("Recebida requisição para salvar histórico");
        HistoriaResponseDTO resultado = service.salvar(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(resultado, "Histórico salvo com sucesso"));
    }

    @Operation(summary = "Obter todo o histórico", description = "Retorna todas as buscas realizadas (ordenadas por data decrescente)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<HistoriaResponseDTO>>> obterTodos() {
        log.info("Recebida requisição para obter histórico completo");
        List<HistoriaResponseDTO> historico = service.obterTodos();
        return ResponseEntity.ok(ApiResponse.success(historico, "Histórico recuperado com sucesso"));
    }

    @Operation(summary = "Deletar item específico do histórico", description = "Remove uma entrada do histórico por ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletarPorId(@PathVariable String id) {
        log.info("Recebida requisição para deletar histórico com ID: {}", id);
        service.deletarPorId(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Histórico deletado com sucesso"));
    }

    @Operation(summary = "Limpar todo o histórico", description = "Remove todas as entradas do histórico")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deletarTodos() {
        log.info("Recebida requisição para limpar histórico completo");
        service.deletarTodos();
        return ResponseEntity.ok(ApiResponse.success(null, "Histórico limpo com sucesso"));
    }
}
