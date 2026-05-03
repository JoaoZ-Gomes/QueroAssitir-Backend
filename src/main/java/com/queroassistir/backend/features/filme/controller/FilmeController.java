package com.queroassistir.backend.features.filme.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.queroassistir.backend.features.filme.dto.MovieResponseDTO;
import com.queroassistir.backend.features.filme.service.FilmeService;
import com.queroassistir.backend.infrastructure.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
@Tag(name = "Filmes", description = "Endpoints para gerenciar filmes")
public class FilmeController {

    private final FilmeService service;

    @Operation(summary = "Listar todos os filmes", description = "Retorna uma lista de todos os filmes cadastrados")
    @GetMapping
    public ResponseEntity<ApiResponse<List<MovieResponseDTO>>> obterTodos() {
        log.info("Recebida requisição para obter todos os filmes");
        List<MovieResponseDTO> filmes = service.obterTodos();
        return ResponseEntity.ok(ApiResponse.success(filmes, "Filmes recuperados com sucesso"));
    }

    @Operation(summary = "Obter filme por ID", description = "Retorna os detalhes de um filme específico")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MovieResponseDTO>> obterPorId(
            @PathVariable @Parameter(description = "ID do filme") String id) {
        log.info("Recebida requisição para obter filme com ID: {}", id);
        MovieResponseDTO filme = service.obterPorId(id);
        return ResponseEntity.ok(ApiResponse.success(filme, "Filme recuperado com sucesso"));
    }

    @Operation(summary = "Buscar filmes por título", description = "Busca filmes cujo título contenha o termo informado")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<MovieResponseDTO>>> buscarPorTitulo(
            @RequestParam @Parameter(description = "Termo de busca") String q) {
        log.info("Recebida requisição para buscar filmes por título: {}", q);
        List<MovieResponseDTO> filmes = service.buscarPorTitulo(q);
        return ResponseEntity.ok(ApiResponse.success(filmes, "Busca realizada com sucesso"));
    }

    @Operation(summary = "Buscar filmes por gênero", description = "Retorna filmes de um gênero específico")
    @GetMapping("/genre/{genero}")
    public ResponseEntity<ApiResponse<List<MovieResponseDTO>>> buscarPorGenero(
            @PathVariable @Parameter(description = "Gênero do filme") String genero) {
        log.info("Recebida requisição para buscar filmes por gênero: {}", genero);
        List<MovieResponseDTO> filmes = service.buscarPorGenero(genero);
        return ResponseEntity.ok(ApiResponse.success(filmes, "Busca por gênero realizada com sucesso"));
    }

    @Operation(summary = "Criar novo filme", description = "Cria um novo registro de filme")
    @PostMapping
    public ResponseEntity<ApiResponse<MovieResponseDTO>> criar(@RequestBody MovieResponseDTO dto) {
        log.info("Recebida requisição para criar novo filme: {}", dto.getTitle());
        MovieResponseDTO filme = service.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(filme, "Filme criado com sucesso"));
    }

    @Operation(summary = "Atualizar filme", description = "Atualiza os dados de um filme existente")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MovieResponseDTO>> atualizar(
            @PathVariable @Parameter(description = "ID do filme") String id,
            @RequestBody MovieResponseDTO dto) {
        log.info("Recebida requisição para atualizar filme com ID: {}", id);
        MovieResponseDTO filme = service.atualizar(id, dto);
        return ResponseEntity.ok(ApiResponse.success(filme, "Filme atualizado com sucesso"));
    }

    @Operation(summary = "Deletar filme", description = "Remove um filme do sistema")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletar(
            @PathVariable @Parameter(description = "ID do filme") String id) {
        log.info("Recebida requisição para deletar filme com ID: {}", id);
        service.deletar(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Filme deletado com sucesso"));
    }
}
