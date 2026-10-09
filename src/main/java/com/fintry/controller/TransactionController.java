package com.fintry.controller;

import com.fintry.dto.TradeRequest;
import com.fintry.dto.TransactionResponse;
import com.fintry.service.TransactionService;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    @org.springframework.beans.factory.annotation.Autowired
    private com.fintry.security.AccountAccess access;

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/buy")
    public TransactionResponse buy(@Valid @RequestBody com.fintry.dto.TradeCommand command) {
        return transactionService.buy(TradeRequest.builder().userId(access.userId()).instrumentId(command.instrumentId()).quantity(command.quantity()).build());
    }

    @PostMapping("/sell")
    public TransactionResponse sell(@Valid @RequestBody com.fintry.dto.TradeCommand command) {
        return transactionService.sell(TradeRequest.builder().userId(access.userId()).instrumentId(command.instrumentId()).quantity(command.quantity()).build());
    }

    @GetMapping("/me")
    public List<TransactionResponse> me() { return transactionService.getTransactionsByUserId(access.userId()); }

    @GetMapping("/user/{userId}")
    public List<TransactionResponse> getTransactionsByUserId(@PathVariable Long userId) {
        return transactionService.getTransactionsByUserId(userId);
    }
}