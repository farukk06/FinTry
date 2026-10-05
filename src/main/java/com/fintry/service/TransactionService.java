package com.fintry.service;

import com.fintry.dto.TradeRequest;
import com.fintry.entity.*;
import com.fintry.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    @Transactional
    public Transaction buy(TradeRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Instrument instrument = instrumentRepository.findById(request.getInstrumentId())
                .orElseThrow(() -> new RuntimeException("Instrument not found"));

        VirtualAccount account = virtualAccountRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Virtual account not found"));

        BigDecimal quantity = request.getQuantity();
        BigDecimal price = instrument.getPrice();
        BigDecimal totalAmount = price.multiply(quantity);

        if (account.getBalance().compareTo(totalAmount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        account.setBalance(account.getBalance().subtract(totalAmount));

        PortfolioAsset asset = portfolioAssetRepository
                .findByUserIdAndInstrumentId(request.getUserId(), request.getInstrumentId())
                .orElse(null);

        if (asset == null) {
            asset = PortfolioAsset.builder()
                    .user(user)
                    .instrument(instrument)
                    .quantity(quantity)
                    .averagePrice(price)
                    .build();
        } else {
            BigDecimal oldQuantity = asset.getQuantity();
            BigDecimal oldAveragePrice = asset.getAveragePrice();
            BigDecimal newQuantity = oldQuantity.add(quantity);

            BigDecimal oldTotal = oldQuantity.multiply(oldAveragePrice);
            BigDecimal newTotal = quantity.multiply(price);

            BigDecimal newAveragePrice = oldTotal
                    .add(newTotal)
                    .divide(newQuantity, 2, RoundingMode.HALF_UP);

            asset.setQuantity(newQuantity);
            asset.setAveragePrice(newAveragePrice);
        }

        Transaction transaction = Transaction.builder()
                .type("BUY")
                .quantity(quantity)
                .price(price)
                .totalAmount(totalAmount)
                .transactionTime(LocalDateTime.now())
                .user(user)
                .instrument(instrument)
                .build();

        portfolioAssetRepository.save(asset);
        virtualAccountRepository.save(account);

        return transactionRepository.save(transaction);
    }

    @Transactional
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

        BigDecimal quantity = request.getQuantity();
        BigDecimal price = instrument.getPrice();

        if (asset.getQuantity().compareTo(quantity) < 0) {
            throw new RuntimeException("Insufficient asset quantity");
        }

        BigDecimal totalAmount = price.multiply(quantity);
        BigDecimal remainingQuantity = asset.getQuantity().subtract(quantity);

        if (remainingQuantity.compareTo(BigDecimal.ZERO) == 0) {
            portfolioAssetRepository.delete(asset);
        } else {
            asset.setQuantity(remainingQuantity);
            portfolioAssetRepository.save(asset);
        }

        account.setBalance(account.getBalance().add(totalAmount));
        virtualAccountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .type("SELL")
                .quantity(quantity)
                .price(price)
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