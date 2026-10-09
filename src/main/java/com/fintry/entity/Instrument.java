package com.fintry.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "instruments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Instrument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String symbol; // THYAO, USDTRY

    @Column(nullable = false)
    private String name; // Türk Hava Yolları, Dolar

    @Column(nullable = false)
    private String type; // STOCK, FOREX, GOLD

    @Column(nullable = false, precision = 44, scale = 8)
    private BigDecimal price; // güncel fiyat
}
