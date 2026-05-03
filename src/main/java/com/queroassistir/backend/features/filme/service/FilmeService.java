package com.queroassistir.backend.features.filme.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.queroassistir.backend.features.filme.dto.MovieResponseDTO;
import com.queroassistir.backend.features.filme.model.Movie;
import com.queroassistir.backend.features.filme.repository.FilmeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilmeService {

    private final FilmeRepository repository;

    public List<MovieResponseDTO> obterTodos() {
        log.info("Recuperando todos os filmes");
        return repository.findAll()
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public MovieResponseDTO obterPorId(String id) {
        log.info("Recuperando filme com ID: {}", id);
        return repository.findById(id)
                .map(this::mapToResponseDTO)
                .orElseThrow(() -> new IllegalArgumentException("Filme não encontrado: " + id));
    }

    public List<MovieResponseDTO> buscarPorTitulo(String titulo) {
        log.info("Buscando filmes por título: {}", titulo);
        return repository.findByTitleContainingIgnoreCase(titulo)
                .stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<MovieResponseDTO> buscarPorGenero(String genero) {
        log.info("Buscando filmes por gênero: {}", genero);
        return repository.findAll()
                .stream()
                .filter(m -> m.getGenres() != null && m.getGenres().stream()
                        .anyMatch(g -> g.equalsIgnoreCase(genero)))
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public MovieResponseDTO criar(MovieResponseDTO dto) {
        log.info("Criando novo filme: {}", dto.getTitle());
        
        Movie filme = Movie.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .image(dto.getImage())
                .rating(dto.getRating())
                .genres(dto.getGenres())
                .duration(dto.getDuration())
                .durationMinutes(dto.getDurationMinutes())
                .releaseYear(dto.getYear())
                .director(dto.getDirector())
                .build();

        Movie salvo = repository.save(filme);
        return mapToResponseDTO(salvo);
    }

    public MovieResponseDTO atualizar(String id, MovieResponseDTO dto) {
        log.info("Atualizando filme com ID: {}", id);
        
        Movie filme = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Filme não encontrado: " + id));

        filme.setTitle(dto.getTitle());
        filme.setDescription(dto.getDescription());
        filme.setImage(dto.getImage());
        filme.setRating(dto.getRating());
        filme.setGenres(dto.getGenres());
        filme.setDuration(dto.getDuration());
        filme.setDurationMinutes(dto.getDurationMinutes());
        filme.setReleaseYear(dto.getYear());
        filme.setDirector(dto.getDirector());

        Movie atualizado = repository.save(filme);
        return mapToResponseDTO(atualizado);
    }

    public void deletar(String id) {
        log.info("Deletando filme com ID: {}", id);
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Filme não encontrado: " + id);
        }
        repository.deleteById(id);
    }

    private MovieResponseDTO mapToResponseDTO(Movie filme) {
        return MovieResponseDTO.builder()
                .id(filme.getId())
                .title(filme.getTitle())
                .description(filme.getDescription())
                .image(filme.getImage())
                .rating(filme.getRating())
                .genres(filme.getGenres())
                .duration(filme.getDuration())
                .durationMinutes(filme.getDurationMinutes())
                .year(filme.getReleaseYear())
                .director(filme.getDirector())
                .platforms(filme.getPlatforms())
                .build();
    }
}
