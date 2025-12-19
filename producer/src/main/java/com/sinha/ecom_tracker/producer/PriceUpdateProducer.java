package com.sinha.ecom_tracker.producer;


public interface PriceUpdateProducer {
    void sendPriceUpdateEvent(PriceUpdateEvent priceUpdateEvent);
}
