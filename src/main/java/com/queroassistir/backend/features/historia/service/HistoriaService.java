package com.queroassistir.backend.features.historia.service;

import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.queroassistir.backend.features.historia.dto.HistoriaRequestDTO;
import com.queroassistir.backend.features.historia.dto.HistoriaResponseDTO;
import com.queroassistir.backend.features.historia.model.Historia;
import com.queroassistir.backend.features.historia.repository.HistoriaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoriaService {

    private final HistoriaRepository repository;

    public HistoriaResponseDTO salvar(HistoriaRequestDTO dto) {
        log.info("Salvando histórico de busca. Mood: {}, Query: {}", dto.getMood(), dto.getQuery());

        Historia historia = Historia.builder()
                .query(dto.getQuery())
                .mood(dto.getMood())
                .context(dto.getContext())
                .duration(dto.getDuration())
                .filmeId(dto.getFilmeId())
                .filmeTitulo(dto.getFilmeTitulo())
                .filmeImagem(dto.getFilmeImagem())
                .build();

        Historia salva = repository.save(historia);
        log.info("Histórico salvo com sucesso. ID: {}", salva.getId());
        
        return mapToResponseDTO(salva);
    }

    public List<HistoriaResponseDTO> obterTodos() {
        log.info("Recuperando histórico completo");
        return repository.findAllByOrderByCriadoEmDesc()
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public void deletarTodos() {
        log.info("Limpando histórico completo");
        repository.deleteAll();
    }

    public void deletarPorId(String id) {
        log.info("Deletando histórico com ID: {}", id);
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Histórico não encontrado: " + id);
        }
        repository.deleteById(id);
    }

    private HistoriaResponseDTO mapToResponseDTO(Historia historia) {
        // Converte LocalDateTime para epoch millis (compatível com Date.now() do frontend)
        Long timestampMillis = historia.getCriadoEm() != null
                ? historia.getCriadoEm().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                : System.currentTimeMillis();

        return HistoriaResponseDTO.builder()
                .id(historia.getId())
                .query(historia.getQuery())
                .mood(historia.getMood())
                .context(historia.getContext())
                .duration(historia.getDuration())
                .movie(HistoriaResponseDTO.HistoriaMovieDTO.builder()
                        .id(historia.getFilmeId())
                        .title(historia.getFilmeTitulo())
                        .image(historia.getFilmeImagem())
                        .build())
                .timestamp(timestampMillis)
                .build();
    }
}
