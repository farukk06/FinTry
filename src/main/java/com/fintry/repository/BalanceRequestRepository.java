package com.fintry.repository;

import com.fintry.entity.BalanceRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BalanceRequestRepository extends JpaRepository<BalanceRequest, Long> {
}