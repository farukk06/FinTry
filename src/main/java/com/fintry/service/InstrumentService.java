package com.fintry.service;

import com.fintry.entity.Instrument;
import com.fintry.repository.InstrumentRepository;
import org.springframework.stereotype.Service;
import com.fintry.exception.ResourceNotFoundException;
import com.fintry.dto.CreateInstrumentRequest;
import com.fintry.dto.InstrumentResponse;

import java.math.BigDecimal;
import java.util.List;

@Service
public class InstrumentService {

    private final InstrumentRepository instrumentRepository;

    public InstrumentService(InstrumentRepository instrumentRepository) {
        this.instrumentRepository = instrumentRepository;
    }

    public InstrumentResponse createInstrument(CreateInstrumentRequest request) {

        Instrument instrument = Instrument.builder()
                .symbol(request.getSymbol())
                .name(request.getName())
                .type(request.getType())
                .price(request.getPrice())
                .build();

        Instrument savedInstrument = instrumentRepository.save(instrument);

        return toResponse(savedInstrument);
    }

    public List<InstrumentResponse> getAllInstruments() {
        return instrumentRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public InstrumentResponse updateInstrumentPrice(Long id, BigDecimal newPrice) {
        Instrument instrument = instrumentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Instrument not found"));

        instrument.setPrice(newPrice);

        Instrument savedInstrument = instrumentRepository.save(instrument);

        return toResponse(savedInstrument);
    }

    private InstrumentResponse toResponse(Instrument instrument) {
        return InstrumentResponse.builder()
                .id(instrument.getId())
                .symbol(instrument.getSymbol())
                .name(instrument.getName())
                .type(instrument.getType())
                .price(instrument.getPrice())
                .build();
    }
}