package com.fintry.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "portfolio_assets", uniqueConstraints = @UniqueConstraint(
        name = "uk_portfolio_user_instrument", columnNames = {"user_id", "instrument_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 44, scale = 8)
    private BigDecimal quantity;

    @Column(name = "average_price", nullable = false, precision = 44, scale = 8)
    private BigDecimal averagePrice;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;
}
