package com.fintry.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeRequest {
    private Long userId;
    private Long instrumentId;
    private Double quantity;
}