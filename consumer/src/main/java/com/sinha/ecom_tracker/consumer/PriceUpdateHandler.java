package com.sinha.ecom_tracker.consumer;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.KafkaEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PriceUpdateHandler implements RequestHandler<KafkaEvent, String> {

    private final S3Client s3Client;
    private final DynamoDbClient dynamoDbClient;
    private final ObjectMapper objectMapper;

    private final String s3Bucket;
    private final String dynamoTable;

    public PriceUpdateHandler() {
        this.s3Client = S3Client.create();
        this.dynamoDbClient = DynamoDbClient.create();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());

        this.s3Bucket = System.getenv("S3_BUCKET");
        this.dynamoTable = System.getenv("DYNAMODB_TABLE");
    }

    @Override
    public String handleRequest(KafkaEvent kafkaEvent, Context context) {

        LambdaLogger logger = context.getLogger();

        int successCount = 0;
        int errorCount = 0;

        // Process each topic-partition
        for(Map.Entry<String, List<KafkaEvent.KafkaEventRecord>> entry : kafkaEvent.getRecords().entrySet()) {
            String topicPartition = entry.getKey();
            logger.log("Processing records from: " + topicPartition);

            for(KafkaEvent.KafkaEventRecord record : entry.getValue()) {
                try {
                    // Step 1 : Decode Base64 value
                    String jsonValue = new String(Base64.getDecoder().decode(record.getValue()));
                    logger.log("Recieved: " + jsonValue);

                    // Step 2 : Deserialize json to PriceUpdateEvent
                    PriceUpdateEvent priceUpdateEvent = objectMapper.readValue(jsonValue, PriceUpdateEvent.class);

                    // Step 3 : Validate the event

                    // Step 4 : update to s3
                    writeTos3(priceUpdateEvent, jsonValue, logger);

                    // Step 5 : write to dynamo
                    updateDynamoDB(priceUpdateEvent, logger);

                    successCount++;

                } catch (Exception e) {
                    logger.log("ERROR processing record : " + e.getMessage());
                    errorCount++;
                }
            }
        }

        String result = String.format("Processed %d successfully, %d errors", successCount, errorCount);
        logger.log(result);

        return result;
    }

    public void writeTos3(PriceUpdateEvent event, String jsonContent, LambdaLogger logger) {
        // Get Partitioned S3 Key
        LocalDateTime ts = event.getTimeStamp() != null ? event.getTimeStamp() : LocalDateTime.now();
        String s3Key = String.format("raw/year=%d/month=%02d/day=%02d/hour=%02d/%s-%s.json",
                ts.getYear(),
                ts.getMonthValue(),
                ts.getDayOfMonth(),
                ts.getHour(),
                event.getProductId(),
                UUID.randomUUID().toString().substring(0, 8));

        // Upload to S3
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Bucket)
                .key(s3Key)
                .contentType("application/json")
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromString(jsonContent));
        logger.log("Wrote to S3: " + s3Key);
    }

    private void updateDynamoDB(PriceUpdateEvent event, LambdaLogger logger) {
        // Create composite keys
        String pk = "PRODUCT#" + event.getProductId();
        String sk = "VENDOR#" + event.getVendorId();

        // Calculate TTL (30 days from now)
        long ttl = Instant.now().plus(30, ChronoUnit.DAYS).getEpochSecond();

        // Determine price direction
        String priceDirection = "STABLE";
        if (event.getOldPrice() != null && event.getNewPrice() != null) {
            int comparison = event.getNewPrice().compareTo(event.getOldPrice());
            if (comparison < 0) priceDirection = "DOWN";
            else if (comparison > 0) priceDirection = "UP";
        }

        // Build update expression
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("PK", AttributeValue.builder().s(pk).build());
        key.put("SK", AttributeValue.builder().s(sk).build());

        Map<String, AttributeValue> values = new HashMap<>();
        values.put(":price", AttributeValue.builder().n(event.getNewPrice().toString()).build());
        values.put(":currency", AttributeValue.builder().s(event.getCurrency().toString()).build());
        values.put(":timestamp", AttributeValue.builder().s(LocalDateTime.now().toString()).build());
        values.put(":direction", AttributeValue.builder().s(priceDirection).build());
        values.put(":ttl", AttributeValue.builder().n(String.valueOf(ttl)).build());
        values.put(":productId", AttributeValue.builder().s(event.getProductId()).build());
        values.put(":vendorId", AttributeValue.builder().s(event.getVendorId()).build());

        UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                .tableName(dynamoTable)
                .key(key)
                .updateExpression("SET currentPrice = :price, currency = :currency, " +
                        "lastUpdated = :timestamp, priceDirection = :direction, " +
                        "productId = :productId, vendorId = :vendorId, ttl = :ttl")
                .expressionAttributeValues(values)
                .build();

        dynamoDbClient.updateItem(updateRequest);
        logger.log("Updated DynamoDB: " + pk + " / " + sk);
    }
}
