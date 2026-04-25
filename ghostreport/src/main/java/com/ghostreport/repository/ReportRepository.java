package com.ghostreport.repository;

import com.ghostreport.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    Optional<Report> findByTrackingCodeHash(String trackingCodeHash);
}