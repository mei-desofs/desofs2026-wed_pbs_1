package com.ghostreport.repository;

import com.ghostreport.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    Optional<Report> findByTrackingCodeHash(String trackingCodeHash);

    @Query("""
            select r from Report r
            left join r.caseReview cr
            left join cr.assignedAnalyst analyst
            where cr is null
               or analyst is null
               or analyst.username = :username
            """)
    List<Report> findVisibleToAnalyst(@Param("username") String username);
}
