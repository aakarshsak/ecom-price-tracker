package com.sinha.ecom_tracker.consumer;

import java.math.BigDecimal;
import java.time.LocalDateTime;


public class PriceUpdateEvent {

    private String productId;
    private String vendorId;
    private BigDecimal oldPrice;
    private BigDecimal newPrice;
    private Currency currency;
    private LocalDateTime timeStamp;
    private EventType eventType;

    public PriceUpdateEvent() {

    }

    public PriceUpdateEvent(String productId, String vendorId, BigDecimal oldPrice, BigDecimal newPrice, Currency currency, LocalDateTime timeStamp, EventType eventType) {
        this.productId = productId;
        this.vendorId = vendorId;
        this.oldPrice = oldPrice;
        this.newPrice = newPrice;
        this.currency = currency;
        this.timeStamp = timeStamp;
        this.eventType = eventType;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getVendorId() {
        return vendorId;
    }

    public void setVendorId(String vendorId) {
        this.vendorId = vendorId;
    }

    public BigDecimal getOldPrice() {
        return oldPrice;
    }

    public void setOldPrice(BigDecimal oldPrice) {
        this.oldPrice = oldPrice;
    }

    public BigDecimal getNewPrice() {
        return newPrice;
    }

    public void setNewPrice(BigDecimal newPrice) {
        this.newPrice = newPrice;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public LocalDateTime getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(LocalDateTime timeStamp) {
        this.timeStamp = timeStamp;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }
}
