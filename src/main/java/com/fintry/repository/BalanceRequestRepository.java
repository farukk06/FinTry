package com.fintry.repository;

import com.fintry.entity.BalanceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface BalanceRequestRepository extends JpaRepository<BalanceRequest, Long> {
    java.util.List<BalanceRequest> findByUserId(Long userId);
    java.util.List<BalanceRequest> findByStatus(com.fintry.entity.BalanceRequestStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from BalanceRequest r where r.id = :id")
    Optional<BalanceRequest> findByIdForUpdate(Long id);
}
