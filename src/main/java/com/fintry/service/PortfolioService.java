package com.fintry.service;

import com.fintry.dto.PortfolioResponse;
import com.fintry.entity.PortfolioAsset;
import com.fintry.repository.PortfolioAssetRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PortfolioService {

    private final PortfolioAssetRepository portfolioAssetRepository;

    public PortfolioService(PortfolioAssetRepository portfolioAssetRepository) {
        this.portfolioAssetRepository = portfolioAssetRepository;
    }

    public List<PortfolioResponse> getUserPortfolio(Long userId) {

        List<PortfolioAsset> assets = portfolioAssetRepository.findByUserId(userId);

        return assets.stream().map(asset -> {

            double currentPrice = asset.getInstrument().getPrice();
            double quantity = asset.getQuantity();
            double averagePrice = asset.getAveragePrice();

            double totalValue = quantity * currentPrice;
            double profitLoss = (currentPrice - averagePrice) * quantity;

            return PortfolioResponse.builder()
                    .symbol(asset.getInstrument().getSymbol())
                    .name(asset.getInstrument().getName())
                    .quantity(quantity)
                    .averagePrice(averagePrice)
                    .currentPrice(currentPrice)
                    .totalValue(totalValue)
                    .profitLoss(profitLoss)
                    .build();

        }).collect(Collectors.toList());
    }
}