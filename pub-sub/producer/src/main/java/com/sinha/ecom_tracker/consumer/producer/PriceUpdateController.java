package com.sinha.ecom_tracker.consumer.producer;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/producers/price-update")
public class PriceUpdateController {
    private PriceUpdateProducer priceUpdateProducer;

    @Autowired
    public PriceUpdateController(PriceUpdateProducer priceUpdateProducer) {
        this.priceUpdateProducer = priceUpdateProducer;
    }

    @PostMapping("")
    public ResponseEntity<String> sendPriceUpdate(@RequestBody PriceUpdateEvent priceUpdateEvent) {
        priceUpdateProducer.sendPriceUpdateEvent(priceUpdateEvent);

        return ResponseEntity.ok("Price update event successfully created.");
    }
}
