package com.sinha.ecom_tracker.producer;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceUpdateEvent {

    private String productId;
    private String vendorId;
    private BigDecimal oldPrice;
    private BigDecimal newPrice;
    private Currency currency;
    private LocalDateTime timeStamp;
    private EventType eventType;
}
