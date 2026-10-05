package com.fintry.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class InstrumentResponse {

    private Long id;
    private String symbol;
    private String name;
    private String type;
    private BigDecimal price;
}