package com.fintry.service;

import com.fintry.entity.User;
import com.fintry.entity.VirtualAccount;
import com.fintry.repository.UserRepository;
import com.fintry.repository.VirtualAccountRepository;
import org.springframework.stereotype.Service;

@Service
public class VirtualAccountService {

    private final VirtualAccountRepository virtualAccountRepository;
    private final UserRepository userRepository;

    public VirtualAccountService(VirtualAccountRepository virtualAccountRepository, UserRepository userRepository) {
        this.virtualAccountRepository = virtualAccountRepository;
        this.userRepository = userRepository;
    }

    public VirtualAccount createVirtualAccount(Long userId, Double balance) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean accountExists = virtualAccountRepository.findByUserId(userId).isPresent();
        if (accountExists) {
            throw new RuntimeException("Virtual account already exists for this user");
        }

        VirtualAccount virtualAccount = VirtualAccount.builder()
                .balance(balance)
                .user(user)
                .build();

        return virtualAccountRepository.save(virtualAccount);
    }

    public VirtualAccount getByUserId(Long userId) {
        return virtualAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Virtual account not found"));
    }
}