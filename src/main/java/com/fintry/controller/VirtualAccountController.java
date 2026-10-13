package com.fintry.controller;

import com.fintry.dto.VirtualAccountResponse;
import com.fintry.service.VirtualAccountService;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/accounts")
public class VirtualAccountController {

    @org.springframework.beans.factory.annotation.Autowired
    private com.fintry.security.AccountAccess access;

    private final VirtualAccountService virtualAccountService;

    public VirtualAccountController(VirtualAccountService virtualAccountService) {
        this.virtualAccountService = virtualAccountService;
    }

    @GetMapping("/me")
    public VirtualAccountResponse me() { return virtualAccountService.getByUserId(access.userId()); }

    @GetMapping("/user/{userId}")
    public VirtualAccountResponse getAccountByUserId(@PathVariable Long userId) {
        return virtualAccountService.getByUserId(userId);
    }
}
