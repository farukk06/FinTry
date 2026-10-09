package com.fintry.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "balance_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_balance_request_user"))
    private User user;

    @Column(nullable = false, precision = 52, scale = 16)
    private BigDecimal requestedAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BalanceRequestStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
