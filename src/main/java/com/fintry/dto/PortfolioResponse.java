package com.fintry.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioResponse {

    private String symbol;
    private String name;

    private BigDecimal quantity;
    private BigDecimal averagePrice;
    private BigDecimal currentPrice;

    private BigDecimal totalValue;
    private BigDecimal profitLoss;
}