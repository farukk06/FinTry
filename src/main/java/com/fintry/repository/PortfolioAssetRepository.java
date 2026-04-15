package com.fintry.repository;

import com.fintry.entity.PortfolioAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PortfolioAssetRepository extends JpaRepository<PortfolioAsset, Long> {
    List<PortfolioAsset> findByUserId(Long userId);
    Optional<PortfolioAsset> findByUserIdAndInstrumentId(Long userId, Long instrumentId);
}