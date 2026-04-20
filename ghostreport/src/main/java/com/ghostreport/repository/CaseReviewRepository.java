package com.ghostreport.repository;

import com.ghostreport.model.CaseReview;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaseReviewRepository extends JpaRepository<CaseReview, Long> {
}