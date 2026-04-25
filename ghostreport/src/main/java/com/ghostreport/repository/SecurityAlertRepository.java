package com.ghostreport.repository;

import com.ghostreport.model.SecurityAlert;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecurityAlertRepository extends JpaRepository<SecurityAlert, Long> {
}