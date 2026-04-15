package com.fintry.controller;

import com.fintry.dto.TradeRequest;
import com.fintry.entity.Transaction;
import com.fintry.service.TransactionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/buy")
    public Transaction buy(@RequestBody TradeRequest request) {
        return transactionService.buy(request);
    }

    @PostMapping("/sell")
    public Transaction sell(@RequestBody TradeRequest request) {
        return transactionService.sell(request);
    }

    @GetMapping("/user/{userId}")
    public List<Transaction> getTransactionsByUserId(@PathVariable Long userId) {
        return transactionService.getTransactionsByUserId(userId);
    }
}