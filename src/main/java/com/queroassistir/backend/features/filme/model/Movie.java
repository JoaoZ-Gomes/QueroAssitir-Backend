package com.queroassistir.backend.features.filme.model;

import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_filmes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Movie {

    @Id
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String image;

    private Double rating;

    @ElementCollection
    @CollectionTable(name = "tb_filme_generos", joinColumns = @JoinColumn(name = "filme_id"))
    @Column(name = "genero")
    private List<String> genres;

    private String duration;

    private Integer durationMinutes;

    private Integer releaseYear;

    private String director;

    @ElementCollection
    @CollectionTable(name = "tb_filme_plataformas", joinColumns = @JoinColumn(name = "filme_id"))
    @Column(name = "plataforma")
    private List<String> platforms;
}
