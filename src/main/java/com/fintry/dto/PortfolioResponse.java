package com.fintry.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioResponse {

    private String symbol;
    private String name;

    private Double quantity;
    private Double averagePrice;
    private Double currentPrice;

    private Double totalValue;
    private Double profitLoss;
}