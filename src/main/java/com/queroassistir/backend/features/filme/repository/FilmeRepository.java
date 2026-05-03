package com.queroassistir.backend.features.filme.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.queroassistir.backend.features.filme.model.Movie;

@Repository
public interface FilmeRepository extends JpaRepository<Movie, String> {
    List<Movie> findByTitleContainingIgnoreCase(String title);
}
