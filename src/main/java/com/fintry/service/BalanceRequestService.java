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

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BalanceRequestService {

    private final BalanceRequestRepository balanceRequestRepository;
    private final VirtualAccountRepository virtualAccountRepository;

    public BalanceRequest createRequest(BalanceRequest request) {
        request.setStatus(BalanceRequestStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());

        return balanceRequestRepository.save(request);
    }

    @Transactional
    public BalanceRequest approveRequest(Long requestId) {
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

        return balanceRequestRepository.save(request);
    }
}