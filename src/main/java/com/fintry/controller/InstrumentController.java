package com.fintry.controller;

import com.fintry.dto.CreateInstrumentRequest;
import com.fintry.dto.InstrumentResponse;
import jakarta.validation.Valid;
import com.fintry.service.InstrumentService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/instruments")
public class InstrumentController {

    private final InstrumentService instrumentService;

    public InstrumentController(InstrumentService instrumentService) {
        this.instrumentService = instrumentService;
    }

    @PostMapping
    public InstrumentResponse createInstrument(
            @Valid @RequestBody CreateInstrumentRequest request) {

        return instrumentService.createInstrument(request);
    }

    @GetMapping
    public List<InstrumentResponse> getAllInstruments() {
        return instrumentService.getAllInstruments();
    }

    @PutMapping("/{id}/price")
    public InstrumentResponse updateInstrumentPrice(
            @PathVariable Long id,
            @RequestParam BigDecimal price) {

        return instrumentService.updateInstrumentPrice(id, price);
    }
}