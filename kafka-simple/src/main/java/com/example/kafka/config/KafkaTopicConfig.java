package com.example.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * =============================================================================
 * KAFKA TOPIC CONFIGURATION
 * =============================================================================
 * 
 * This class creates Kafka topics when your application starts.
 * 
 * WHY CREATE TOPICS PROGRAMMATICALLY?
 * ------------------------------------
 * 1. Ensures topic exists before producer/consumer tries to use it
 * 2. You control the configuration (partitions, replication)
 * 3. Version controlled with your code
 * 4. No manual setup required
 * 
 * =============================================================================
 * TOPIC CONCEPTS EXPLAINED
 * =============================================================================
 * 
 * TOPIC:
 * ------
 * - A category or feed name to which messages are published
 * - Like a "table" in a database, or a "folder" for messages
 * - Example topics: "orders", "user-events", "notifications"
 * 
 * PARTITION:
 * ----------
 * - A topic is split into partitions for parallelism
 * - Each partition is an ordered, immutable sequence of messages
 * - Messages within a partition have a unique offset (0, 1, 2, 3...)
 * 
 *   Topic: "messages" with 3 partitions:
 *   
 *   Partition 0: [msg0, msg3, msg6, msg9 ...]  ← offset 0, 1, 2, 3...
 *   Partition 1: [msg1, msg4, msg7, msg10...]  ← offset 0, 1, 2, 3...
 *   Partition 2: [msg2, msg5, msg8, msg11...]  ← offset 0, 1, 2, 3...
 * 
 * WHY PARTITIONS?
 * ---------------
 * 1. PARALLELISM: Multiple consumers can read from different partitions simultaneously
 * 2. SCALABILITY: More partitions = more throughput
 * 3. ORDERING: Messages in SAME partition are ordered
 *              Messages across partitions have NO ordering guarantee
 * 
 * REPLICATION FACTOR:
 * -------------------
 * - How many copies of each partition exist across brokers
 * - replication-factor=3 means each partition is copied to 3 brokers
 * - If one broker dies, data is safe on other brokers
 * - For single broker (local dev): must be 1
 * - For production: typically 3
 * 
 * =============================================================================
 */
@Configuration  // Tells Spring this class contains bean definitions
public class KafkaTopicConfig {

    // Topic name - we'll use this constant everywhere
    public static final String TOPIC_NAME = "messages";
    
    /**
     * Creates a Kafka topic named "messages"
     * 
     * @Bean means Spring will:
     * 1. Call this method at startup
     * 2. Store the returned NewTopic object
     * 3. Use it to create the topic in Kafka (if it doesn't exist)
     */
    @Bean
    public NewTopic messagesTopic() {
        return TopicBuilder
                .name(TOPIC_NAME)           // Topic name: "messages"
                .partitions(3)              // 3 partitions for parallelism
                .replicas(1)                // 1 replica (single broker setup)
                // .configs(...)            // Can add more configs like retention
                .build();
        
        /*
         * After this topic is created, it will look like:
         * 
         * Topic: messages
         * ├── Partition 0 (Leader: Broker 1)
         * ├── Partition 1 (Leader: Broker 1)
         * └── Partition 2 (Leader: Broker 1)
         * 
         * With 3 partitions, you can have up to 3 consumers in a group
         * processing messages in parallel!
         */
    }
    
    /*
     * =========================================================================
     * TOPIC NAMING CONVENTIONS (Best Practices)
     * =========================================================================
     * 
     * Good names:
     * - orders
     * - user-events
     * - payment-transactions
     * - inventory-updates
     * 
     * Bad names:
     * - topic1 (not descriptive)
     * - myTopic (use kebab-case, not camelCase)
     * - ORDERS (avoid uppercase)
     * 
     * Pattern for microservices:
     * - {service-name}.{entity}.{event-type}
     * - Example: order-service.orders.created
     * 
     * =========================================================================
     */
}
