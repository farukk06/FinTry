package com.fintry.service;

import com.fintry.dto.PortfolioResponse;
import com.fintry.entity.PortfolioAsset;
import com.fintry.repository.PortfolioAssetRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PortfolioService {

    private final PortfolioAssetRepository portfolioAssetRepository;

    public PortfolioService(PortfolioAssetRepository portfolioAssetRepository) {
        this.portfolioAssetRepository = portfolioAssetRepository;
    }

    public List<PortfolioResponse> getUserPortfolio(Long userId) {

        List<PortfolioAsset> assets =
                portfolioAssetRepository.findByUserId(userId);

        return assets.stream().map(asset -> {

            BigDecimal currentPrice = asset.getInstrument().getPrice();
            BigDecimal quantity = asset.getQuantity();
            BigDecimal averagePrice = asset.getAveragePrice();

            BigDecimal totalValue =
                    quantity.multiply(currentPrice);

            BigDecimal profitLoss =
                    currentPrice
                            .subtract(averagePrice)
                            .multiply(quantity);

            return PortfolioResponse.builder()
                    .symbol(asset.getInstrument().getSymbol())
                    .name(asset.getInstrument().getName())
                    .quantity(quantity)
                    .averagePrice(averagePrice)
                    .currentPrice(currentPrice)
                    .totalValue(totalValue)
                    .profitLoss(profitLoss)
                    .build();

        }).toList();
    }
}