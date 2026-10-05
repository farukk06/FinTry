package com.fintry.service;

import jakarta.transaction.Transactional;
import com.fintry.entity.BalanceRequest;
import com.fintry.entity.BalanceRequestStatus;
import com.fintry.entity.VirtualAccount;
import com.fintry.repository.BalanceRequestRepository;
import com.fintry.repository.VirtualAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.fintry.exception.BusinessRuleException;
import com.fintry.exception.ResourceNotFoundException;
import com.fintry.dto.CreateBalanceRequest;
import com.fintry.dto.BalanceRequestResponse;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BalanceRequestService {

    private final BalanceRequestRepository balanceRequestRepository;
    private final VirtualAccountRepository virtualAccountRepository;

    public BalanceRequestResponse createRequest(CreateBalanceRequest request) {

        BalanceRequest balanceRequest = BalanceRequest.builder()
                .userId(request.getUserId())
                .requestedAmount(request.getRequestedAmount())
                .status(BalanceRequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        BalanceRequest savedRequest =
                balanceRequestRepository.save(balanceRequest);

        return toResponse(savedRequest);
    }

    @Transactional
    public BalanceRequestResponse approveRequest(Long requestId) {
        BalanceRequest request = balanceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Talep bulunamadı"));

        if (request.getStatus() != BalanceRequestStatus.PENDING) {
            throw new BusinessRuleException("Bu talep zaten işlenmiş");
        }

        VirtualAccount account = virtualAccountRepository
                .findByUserId(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Virtual account bulunamadı"));

        account.setBalance(
                account.getBalance().add(request.getRequestedAmount())
        );

        request.setStatus(BalanceRequestStatus.APPROVED);

        virtualAccountRepository.save(account);

        BalanceRequest savedRequest = balanceRequestRepository.save(request);

        return toResponse(savedRequest);
    }
    private BalanceRequestResponse toResponse(BalanceRequest request) {
        return BalanceRequestResponse.builder()
                .id(request.getId())
                .userId(request.getUserId())
                .requestedAmount(request.getRequestedAmount())
                .status(request.getStatus())
                .createdAt(request.getCreatedAt())
                .build();
    }
}