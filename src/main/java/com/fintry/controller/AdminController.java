package com.fintry.controller;
import com.fintry.dto.BalanceRequestResponse;
import com.fintry.service.BalanceRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final BalanceRequestService requests;
    @GetMapping("/balance-requests")
    public java.util.List<BalanceRequestResponse> pendingRequests() { return requests.pendingRequests(); }
}
