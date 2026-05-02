package com.queroassistir.backend.features.filme.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieResponseDTO {
    private String id;
    private String title;
    private String description;
    private String image;
    private Double rating;
    private List<String> genres;
    private String duration;
    private Integer durationMinutes;
    private Integer year;
    private String director;
    private List<String> platforms;
}
