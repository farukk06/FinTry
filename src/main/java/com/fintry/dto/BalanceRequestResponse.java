package com.fintry.dto;

import com.fintry.entity.BalanceRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class BalanceRequestResponse {

    private Long id;
    private Long userId;
    private BigDecimal requestedAmount;
    private BalanceRequestStatus status;
    private LocalDateTime createdAt;
}