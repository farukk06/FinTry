package com.fintry.service;

import com.fintry.entity.Instrument;
import com.fintry.repository.InstrumentRepository;
import org.springframework.stereotype.Service;

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

    public Instrument updateInstrumentPrice(Long id, Double newPrice) {
        Instrument instrument = instrumentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Instrument not found"));

        instrument.setPrice(newPrice);

        return instrumentRepository.save(instrument);
    }
}