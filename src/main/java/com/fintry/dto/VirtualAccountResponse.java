package com.fintry.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class VirtualAccountResponse {

    private Long id;
    private Long userId;
    private BigDecimal balance;
}