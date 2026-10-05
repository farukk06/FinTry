package com.fintry.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeRequest {
    private Long userId;
    private Long instrumentId;
    private BigDecimal quantity;
}