package com.example.kafka.producer;

import com.example.kafka.config.KafkaTopicConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * =============================================================================
 * KAFKA MESSAGE PRODUCER
 * =============================================================================
 * 
 * This class is responsible for SENDING messages to Kafka.
 * 
 * WHAT IS A PRODUCER?
 * -------------------
 * - A producer is any application that publishes messages to Kafka
 * - It connects to Kafka broker and writes messages to topics
 * - Messages can have: key (optional), value (required), headers (optional)
 * 
 * =============================================================================
 * HOW DOES A MESSAGE GET TO A PARTITION?
 * =============================================================================
 * 
 * When you send a message, Kafka decides which partition it goes to:
 * 
 * 1. IF you provide a KEY:
 *    → Kafka hashes the key: partition = hash(key) % num_partitions
 *    → Same key ALWAYS goes to same partition (ordering guarantee!)
 *    
 *    Example: key="user-123" always goes to partition 2
 *             key="user-456" always goes to partition 1
 * 
 * 2. IF no key (key is null):
 *    → Kafka uses "sticky partitioning" (batches to same partition)
 *    → Or round-robin in older versions
 *    → NO ordering guarantee!
 * 
 * =============================================================================
 * KAFKATEMPLATE
 * =============================================================================
 * 
 * KafkaTemplate is Spring's wrapper around Kafka Producer.
 * It provides convenient methods:
 * 
 * - send(topic, value)              → Send message without key
 * - send(topic, key, value)         → Send message with key
 * - send(topic, partition, key, value) → Send to specific partition
 * - sendDefault(value)              → Send to default topic
 * 
 * =============================================================================
 */
@Service                    // Marks this as a Spring Service bean
@Slf4j                      // Lombok: creates a logger named "log"
@RequiredArgsConstructor    // Lombok: generates constructor for final fields
public class MessageProducer {

    /**
     * KafkaTemplate<K, V> where:
     * - K = Key type (String in our case)
     * - V = Value type (String in our case)
     * 
     * Spring auto-creates this bean based on application.yml configuration
     */
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * Send a message WITHOUT a key
     * 
     * Use this when:
     * - You don't care about ordering
     * - You want load balancing across partitions
     */
    public void sendMessage(String message) {
        log.info("📤 Sending message: {}", message);
        
        // send() returns a CompletableFuture - the send is ASYNC!
        CompletableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(KafkaTopicConfig.TOPIC_NAME, message);
        
        // Add callback to know if send succeeded or failed
        future.whenComplete((result, exception) -> {
            if (exception == null) {
                // SUCCESS!
                log.info("✅ Message sent successfully!");
                log.info("   Topic: {}", result.getRecordMetadata().topic());
                log.info("   Partition: {}", result.getRecordMetadata().partition());
                log.info("   Offset: {}", result.getRecordMetadata().offset());
                /*
                 * What is Offset?
                 * - Each message in a partition gets a unique, sequential number
                 * - First message = offset 0, second = offset 1, etc.
                 * - Offsets are NEVER reused (even if messages are deleted)
                 * - Consumers use offsets to track what they've read
                 */
            } else {
                // FAILURE!
                log.error("❌ Failed to send message: {}", exception.getMessage());
            }
        });
    }

    /**
     * Send a message WITH a key
     * 
     * Use this when:
     * - You need ordering for related messages
     * - Example: All messages for "user-123" should be processed in order
     * 
     * HOW IT WORKS:
     * - Kafka hashes the key to determine partition
     * - Same key → Same partition → Guaranteed ordering
     * 
     * REAL-WORLD EXAMPLE:
     * - Topic: "orders"
     * - Key: customerId
     * - All orders for customer "CUST-001" go to same partition
     * - Consumer processes them in order: order1 → order2 → order3
     */
    public void sendMessageWithKey(String key, String message) {
        log.info("📤 Sending message with key: {} -> {}", key, message);
        
        // Notice: send(topic, KEY, value)
        CompletableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(KafkaTopicConfig.TOPIC_NAME, key, message);
        
        future.whenComplete((result, exception) -> {
            if (exception == null) {
                log.info("✅ Message sent successfully!");
                log.info("   Key: {}", key);
                log.info("   Partition: {}", result.getRecordMetadata().partition());
                log.info("   Offset: {}", result.getRecordMetadata().offset());
                
                // Notice: same key will ALWAYS go to same partition!
            } else {
                log.error("❌ Failed to send message: {}", exception.getMessage());
            }
        });
    }

    /**
     * Send a message to a SPECIFIC partition
     * 
     * Use this when:
     * - You need explicit control over partition assignment
     * - Rarely used in practice (let Kafka decide!)
     */
    public void sendMessageToPartition(int partition, String key, String message) {
        log.info("📤 Sending message to partition {}: {}", partition, message);
        
        // Notice: send(topic, PARTITION, key, value)
        kafkaTemplate.send(KafkaTopicConfig.TOPIC_NAME, partition, key, message)
                .whenComplete((result, exception) -> {
                    if (exception == null) {
                        log.info("✅ Message sent to partition {} at offset {}", 
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("❌ Failed: {}", exception.getMessage());
                    }
                });
    }
}

/*
 * =============================================================================
 * PRODUCER DELIVERY GUARANTEES
 * =============================================================================
 * 
 * Configured via "acks" in application.yml:
 * 
 * acks=0 (Fire and Forget)
 * ------------------------
 * - Producer doesn't wait for any acknowledgment
 * - Fastest, but can lose messages
 * - Use for: metrics, logs (where some loss is OK)
 * 
 * acks=1 (Leader Acknowledgment)
 * ------------------------------
 * - Producer waits for leader broker to acknowledge
 * - Balanced speed and durability
 * - Can lose data if leader crashes before replication
 * 
 * acks=all (Full Acknowledgment) ← WE USE THIS
 * ----------------------------------------------
 * - Producer waits for ALL replicas to acknowledge
 * - Slowest, but strongest guarantee
 * - Use for: financial transactions, orders
 * 
 * =============================================================================
 * 
 * IDEMPOTENT PRODUCER (we enabled this)
 * =====================================
 * 
 * Problem: Network glitch → Producer retries → Duplicate message!
 * 
 * Solution: enable.idempotence=true
 * - Kafka assigns a sequence number to each message
 * - If producer retries, Kafka detects duplicate and ignores it
 * - Result: Exactly-once delivery semantics!
 * 
 * =============================================================================
 */
