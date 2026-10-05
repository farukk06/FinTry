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

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/buy")
    public TransactionResponse buy(@Valid @RequestBody TradeRequest request) {
        return transactionService.buy(request);
    }

    @PostMapping("/sell")
    public TransactionResponse sell(@Valid @RequestBody TradeRequest request) {
        return transactionService.sell(request);
    }

    @GetMapping("/user/{userId}")
    public List<TransactionResponse> getTransactionsByUserId(@PathVariable Long userId) {
        return transactionService.getTransactionsByUserId(userId);
    }
}