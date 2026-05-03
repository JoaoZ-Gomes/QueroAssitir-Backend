package com.queroassistir.backend.features.historia.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_historicos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Historia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String query;

    @Column(nullable = false)
    private String mood;

    @Column(nullable = false)
    private String context;

    private String duration;

    @Column(nullable = false)
    private String filmeId;

    @Column(nullable = false)
    private String filmeTitulo;

    private String filmeImagem;

    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    protected void onCreate() {
        criadoEm = LocalDateTime.now();
    }
}
