package com.sinha.ecom_tracker.consumer.producer;


public interface PriceUpdateProducer {
    void sendPriceUpdateEvent(PriceUpdateEvent priceUpdateEvent);
}
