package com.fintry.service;

import com.fintry.dto.TradeRequest;
import com.fintry.entity.*;
import com.fintry.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final InstrumentRepository instrumentRepository;
    private final VirtualAccountRepository virtualAccountRepository;
    private final PortfolioAssetRepository portfolioAssetRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              UserRepository userRepository,
                              InstrumentRepository instrumentRepository,
                              VirtualAccountRepository virtualAccountRepository,
                              PortfolioAssetRepository portfolioAssetRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.instrumentRepository = instrumentRepository;
        this.virtualAccountRepository = virtualAccountRepository;
        this.portfolioAssetRepository = portfolioAssetRepository;
    }

    public Transaction buy(TradeRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Instrument instrument = instrumentRepository.findById(request.getInstrumentId())
                .orElseThrow(() -> new RuntimeException("Instrument not found"));

        VirtualAccount account = virtualAccountRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Virtual account not found"));

        double totalAmount = instrument.getPrice() * request.getQuantity();

        if (account.getBalance() < totalAmount) {
            throw new RuntimeException("Insufficient balance");
        }

        account.setBalance(account.getBalance() - totalAmount);
        virtualAccountRepository.save(account);

        PortfolioAsset asset = portfolioAssetRepository
                .findByUserIdAndInstrumentId(request.getUserId(), request.getInstrumentId())
                .orElse(null);

        if (asset == null) {
            asset = PortfolioAsset.builder()
                    .user(user)
                    .instrument(instrument)
                    .quantity(request.getQuantity())
                    .averagePrice(instrument.getPrice())
                    .build();
        } else {
            double oldQuantity = asset.getQuantity();
            double oldAveragePrice = asset.getAveragePrice();
            double newQuantity = oldQuantity + request.getQuantity();

            double newAveragePrice =
                    ((oldQuantity * oldAveragePrice) + (request.getQuantity() * instrument.getPrice()))
                            / newQuantity;

            asset.setQuantity(newQuantity);
            asset.setAveragePrice(newAveragePrice);
        }

        portfolioAssetRepository.save(asset);

        Transaction transaction = Transaction.builder()
                .type("BUY")
                .quantity(request.getQuantity())
                .price(instrument.getPrice())
                .totalAmount(totalAmount)
                .transactionTime(LocalDateTime.now())
                .user(user)
                .instrument(instrument)
                .build();

        return transactionRepository.save(transaction);
    }

    public Transaction sell(TradeRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Instrument instrument = instrumentRepository.findById(request.getInstrumentId())
                .orElseThrow(() -> new RuntimeException("Instrument not found"));

        VirtualAccount account = virtualAccountRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Virtual account not found"));

        PortfolioAsset asset = portfolioAssetRepository
                .findByUserIdAndInstrumentId(request.getUserId(), request.getInstrumentId())
                .orElseThrow(() -> new RuntimeException("Portfolio asset not found"));

        if (asset.getQuantity() < request.getQuantity()) {
            throw new RuntimeException("Insufficient asset quantity");
        }

        double totalAmount = instrument.getPrice() * request.getQuantity();

        asset.setQuantity(asset.getQuantity() - request.getQuantity());

        if (asset.getQuantity() == 0) {
            portfolioAssetRepository.delete(asset);
        } else {
            portfolioAssetRepository.save(asset);
        }

        account.setBalance(account.getBalance() + totalAmount);
        virtualAccountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .type("SELL")
                .quantity(request.getQuantity())
                .price(instrument.getPrice())
                .totalAmount(totalAmount)
                .transactionTime(LocalDateTime.now())
                .user(user)
                .instrument(instrument)
                .build();

        return transactionRepository.save(transaction);
    }

    public List<Transaction> getTransactionsByUserId(Long userId) {
        return transactionRepository.findByUserId(userId);
    }
}