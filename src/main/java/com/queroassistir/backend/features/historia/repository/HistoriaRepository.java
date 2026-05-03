package com.queroassistir.backend.features.historia.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.queroassistir.backend.features.historia.model.Historia;

@Repository
public interface HistoriaRepository extends JpaRepository<Historia, String> {
    List<Historia> findAllByOrderByCriadoEmDesc();
    Page<Historia> findAllByOrderByCriadoEmDesc(Pageable pageable);
}
