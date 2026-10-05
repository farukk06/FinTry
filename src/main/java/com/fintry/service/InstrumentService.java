package com.fintry.service;

import com.fintry.entity.Instrument;
import com.fintry.repository.InstrumentRepository;
import org.springframework.stereotype.Service;
import com.fintry.exception.ResourceNotFoundException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class InstrumentService {

    private final InstrumentRepository instrumentRepository;

    public InstrumentService(InstrumentRepository instrumentRepository) {
        this.instrumentRepository = instrumentRepository;
    }

    public Instrument createInstrument(Instrument instrument) {
        return instrumentRepository.save(instrument);
    }

    public List<Instrument> getAllInstruments() {
        return instrumentRepository.findAll();
    }

    public Instrument updateInstrumentPrice(Long id, BigDecimal newPrice) {
        Instrument instrument = instrumentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Instrument not found"));

        instrument.setPrice(newPrice);

        return instrumentRepository.save(instrument);
    }
}