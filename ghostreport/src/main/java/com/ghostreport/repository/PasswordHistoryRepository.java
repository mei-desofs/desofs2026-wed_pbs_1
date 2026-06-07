package com.ghostreport.repository;

import com.ghostreport.model.PasswordHistory;
import com.ghostreport.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, Long> {
    List<PasswordHistory> findTop5ByUserOrderByCreatedAtDesc(User user);
}
