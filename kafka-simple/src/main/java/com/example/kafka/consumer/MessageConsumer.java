package com.example.kafka.consumer;

import com.example.kafka.config.KafkaTopicConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * =============================================================================
 * KAFKA MESSAGE CONSUMER
 * =============================================================================
 * 
 * This class RECEIVES messages from Kafka.
 * 
 * WHAT IS A CONSUMER?
 * -------------------
 * - A consumer is any application that reads messages from Kafka topics
 * - It subscribes to one or more topics
 * - It processes messages and commits offsets to track progress
 * 
 * =============================================================================
 * HOW DOES CONSUMING WORK?
 * =============================================================================
 * 
 * 1. Consumer subscribes to topic "messages"
 * 2. Kafka assigns partitions to this consumer
 * 3. Consumer polls for new messages
 * 4. Consumer processes each message
 * 5. Consumer commits offset (tells Kafka "I've read up to here")
 * 6. Repeat from step 3
 * 
 * =============================================================================
 * @KafkaListener EXPLAINED
 * =============================================================================
 * 
 * This annotation does A LOT behind the scenes:
 * 
 * 1. Creates a Kafka consumer
 * 2. Subscribes to the specified topic(s)
 * 3. Polls for messages in a background thread
 * 4. Deserializes messages (bytes → String)
 * 5. Calls your method for each message
 * 6. Commits offsets (if auto-commit is enabled)
 * 
 * =============================================================================
 * CONSUMER GROUPS - THE KEY CONCEPT!
 * =============================================================================
 * 
 * groupId = "my-first-consumer-group" (from application.yml)
 * 
 * SCENARIO 1: Single Consumer in Group
 * -------------------------------------
 * Consumer A (group: my-first-consumer-group)
 *   └── Gets ALL 3 partitions
 * 
 * SCENARIO 2: Multiple Consumers in SAME Group
 * ---------------------------------------------
 * Consumer A (group: my-first-consumer-group)
 *   └── Gets Partition 0
 * Consumer B (group: my-first-consumer-group)
 *   └── Gets Partition 1, 2
 * 
 * Each message is processed by ONLY ONE consumer in the group!
 * 
 * SCENARIO 3: Consumers in DIFFERENT Groups
 * ------------------------------------------
 * Consumer A (group: group-1)
 *   └── Gets ALL messages
 * Consumer B (group: group-2)
 *   └── Gets ALL messages (same messages!)
 * 
 * Different groups = Each gets a copy of all messages!
 * 
 * =============================================================================
 */
@Service
@Slf4j
public class MessageConsumer {

    /**
     * SIMPLE LISTENER - Just receives the message value
     * 
     * @KafkaListener attributes:
     * - topics: Which topic(s) to subscribe to
     * - groupId: Consumer group (uses default from application.yml if not specified)
     */
    @KafkaListener(
            topics = KafkaTopicConfig.TOPIC_NAME,   // Subscribe to "messages" topic
            groupId = "my-first-consumer-group"      // Consumer group ID
    )
    public void listen(String message) {
        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║  📩 MESSAGE RECEIVED!                                       ║");
        log.info("╠════════════════════════════════════════════════════════════╣");
        log.info("║  Message: {}", message);
        log.info("╚════════════════════════════════════════════════════════════╝");
        
        // =====================================================================
        // YOUR BUSINESS LOGIC GOES HERE!
        // =====================================================================
        // In a real application, you would:
        // - Parse the message (JSON → Object)
        // - Validate the data
        // - Save to database
        // - Call other services
        // - etc.
        // =====================================================================
        
        processMessage(message);
    }
    
    /**
     * DETAILED LISTENER - Gets full ConsumerRecord with metadata
     * 
     * ConsumerRecord contains:
     * - topic: Which topic the message came from
     * - partition: Which partition
     * - offset: Message offset in that partition
     * - key: Message key (null if not provided)
     * - value: The actual message
     * - timestamp: When message was produced
     * - headers: Optional metadata
     * 
     * Uncomment this method to use instead of the simple listener above
     * (Keep only ONE listener active to avoid duplicate processing)
     */
    // @KafkaListener(topics = KafkaTopicConfig.TOPIC_NAME, groupId = "detailed-consumer-group")
    public void listenWithDetails(ConsumerRecord<String, String> record) {
        log.info("╔════════════════════════════════════════════════════════════╗");
        log.info("║  📩 MESSAGE RECEIVED (Detailed)                            ║");
        log.info("╠════════════════════════════════════════════════════════════╣");
        log.info("║  Topic:     {}", record.topic());
        log.info("║  Partition: {}", record.partition());
        log.info("║  Offset:    {}", record.offset());
        log.info("║  Key:       {}", record.key());
        log.info("║  Value:     {}", record.value());
        log.info("║  Timestamp: {}", record.timestamp());
        log.info("╚════════════════════════════════════════════════════════════╝");
    }
    
    /**
     * Simulate message processing
     * In real app, this would be your business logic
     */
    private void processMessage(String message) {
        try {
            // Simulate some processing time
            Thread.sleep(100);
            log.info("✅ Message processed successfully: {}", message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("❌ Processing interrupted: {}", e.getMessage());
        }
    }
}

/*
 * =============================================================================
 * OFFSET MANAGEMENT
 * =============================================================================
 * 
 * AUTO COMMIT (Current Setup - enable-auto-commit: true)
 * -------------------------------------------------------
 * - Kafka automatically commits offsets every X ms
 * - Simple but risky:
 *   → Message received at offset 5
 *   → Auto-commit happens (offset 5 committed)
 *   → Processing fails!
 *   → Consumer restarts at offset 6
 *   → Message at offset 5 is LOST!
 * 
 * MANUAL COMMIT (Production Recommended)
 * --------------------------------------
 * - You commit only AFTER successful processing
 * - Requires: enable-auto-commit: false
 * - And using Acknowledgment parameter:
 * 
 *   @KafkaListener(...)
 *   public void listen(String msg, Acknowledgment ack) {
 *       process(msg);          // Process first
 *       ack.acknowledge();     // Then commit
 *   }
 * 
 * =============================================================================
 * 
 * CONSUMER REBALANCING
 * =============================================================================
 * 
 * When does rebalancing happen?
 * 1. New consumer joins the group
 * 2. Consumer leaves the group (crash or shutdown)
 * 3. New partitions added to topic
 * 4. Consumer fails to send heartbeat
 * 
 * What happens during rebalancing?
 * 1. All consumers stop processing
 * 2. Kafka reassigns partitions
 * 3. Consumers resume from last committed offset
 * 
 * Example:
 * - 3 partitions, 2 consumers
 * - Consumer A: partition 0, 1
 * - Consumer B: partition 2
 * - Consumer B crashes!
 * - REBALANCE →
 * - Consumer A: partition 0, 1, 2 (takes over!)
 * 
 * =============================================================================
 */
