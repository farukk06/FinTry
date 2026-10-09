package com.fintry.controller;

import com.fintry.dto.VirtualAccountResponse;
import com.fintry.service.VirtualAccountService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/accounts")
public class VirtualAccountController {

    private final VirtualAccountService virtualAccountService;

    public VirtualAccountController(VirtualAccountService virtualAccountService) {
        this.virtualAccountService = virtualAccountService;
    }

    @PostMapping("/user/{userId}")
    public VirtualAccountResponse createVirtualAccount(@PathVariable Long userId,
            @RequestParam @jakarta.validation.constraints.PositiveOrZero
            @com.fintry.validation.FinancialPrecision(money = true) BigDecimal balance) {
        return virtualAccountService.createVirtualAccount(userId, balance);
    }

    @GetMapping("/user/{userId}")
    public VirtualAccountResponse getAccountByUserId(@PathVariable Long userId) {
        return virtualAccountService.getByUserId(userId);
    }
}
