package com.fintry.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false, precision = 44, scale = 8)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 44, scale = 8)
    private BigDecimal price;

    @Column(name = "total_amount", nullable = false, precision = 52, scale = 16)
    private BigDecimal totalAmount;

    @Column(name = "transaction_time", nullable = false)
    private LocalDateTime transactionTime;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;
}
