package com.ghostreport.repository;

import com.ghostreport.model.CaseReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CaseReviewRepository extends JpaRepository<CaseReview, Long> {
    Optional<CaseReview> findByReportId(Long reportId);
}