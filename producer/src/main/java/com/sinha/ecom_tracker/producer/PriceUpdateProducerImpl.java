package com.sinha.ecom_tracker.producer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Component
public class PriceUpdateProducerImpl implements PriceUpdateProducer {

    private KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topic.price-updates}")
    private String TOPIC;

    @Autowired
    public PriceUpdateProducerImpl(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendPriceUpdateEvent(PriceUpdateEvent priceUpdateEvent) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(TOPIC, priceUpdateEvent.getProductId(), priceUpdateEvent);

        future.whenComplete((res, ex) -> {
            if(ex==null) {
                System.out.println("Price event sent successfully product id: " + priceUpdateEvent.getProductId()
                        + " at partition: " + res.getRecordMetadata().partition() + " with offset: " + res.getRecordMetadata().offset());
            } else {
                System.out.println("Price event failed for product id: " + priceUpdateEvent.getProductId()
                        + " at partition: " + res.getRecordMetadata().partition());
            }
        });
    }
}
