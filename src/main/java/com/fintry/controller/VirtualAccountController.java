package com.fintry.controller;

import com.fintry.entity.VirtualAccount;
import com.fintry.service.VirtualAccountService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
public class VirtualAccountController {

    private final VirtualAccountService virtualAccountService;

    public VirtualAccountController(VirtualAccountService virtualAccountService) {
        this.virtualAccountService = virtualAccountService;
    }

    @PostMapping("/user/{userId}")
    public VirtualAccount createVirtualAccount(@PathVariable Long userId, @RequestParam Double balance) {
        return virtualAccountService.createVirtualAccount(userId, balance);
    }

    @GetMapping("/user/{userId}")
    public VirtualAccount getAccountByUserId(@PathVariable Long userId) {
        return virtualAccountService.getByUserId(userId);
    }
}