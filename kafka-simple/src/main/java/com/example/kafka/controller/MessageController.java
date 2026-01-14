package com.example.kafka.controller;

import com.example.kafka.producer.MessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * =============================================================================
 * MESSAGE CONTROLLER - REST API ENDPOINTS
 * =============================================================================
 * 
 * This controller provides HTTP endpoints to send messages to Kafka.
 * 
 * FLOW:
 * -----
 * 1. Client sends HTTP POST to /api/send?message=Hello
 * 2. Controller receives the request
 * 3. Calls MessageProducer to send to Kafka
 * 4. Returns success response to client
 * 5. (Meanwhile) Consumer receives the message from Kafka
 * 
 * =============================================================================
 * ENDPOINTS SUMMARY
 * =============================================================================
 * 
 * POST /api/send?message=Hello
 *   → Send message without key (round-robin to partitions)
 * 
 * POST /api/send-with-key?key=user123&message=Hello
 *   → Send message with key (same key → same partition)
 * 
 * POST /api/send-to-partition?partition=0&message=Hello
 *   → Send to specific partition (rarely used)
 * 
 * POST /api/send-batch
 *   → Send multiple messages at once
 * 
 * =============================================================================
 */
@RestController                     // Marks this as a REST controller
@RequestMapping("/api")             // Base path for all endpoints
@RequiredArgsConstructor            // Lombok: generates constructor
@Slf4j                              // Lombok: generates logger
public class MessageController {

    private final MessageProducer messageProducer;

    /**
     * SEND MESSAGE WITHOUT KEY
     * 
     * Usage:
     *   curl -X POST "http://localhost:8080/api/send?message=Hello%20World"
     * 
     * What happens:
     * - Message goes to one of the 3 partitions (round-robin)
     * - No ordering guarantee across partitions
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendMessage(
            @RequestParam String message) {
        
        log.info("📨 REST API: Received request to send message: {}", message);
        
        // Send to Kafka
        messageProducer.sendMessage(message);
        
        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("note", "Message sent to Kafka (check console for consumer logs)");
        
        return ResponseEntity.ok(response);
    }

    /**
     * SEND MESSAGE WITH KEY
     * 
     * Usage:
     *   curl -X POST "http://localhost:8080/api/send-with-key?key=user123&message=Hello"
     * 
     * What happens:
     * - Kafka hashes the key to determine partition
     * - Same key ALWAYS goes to same partition
     * - Guarantees ordering for that key!
     * 
     * Real-world use cases:
     * - key = userId → All events for a user are ordered
     * - key = orderId → All updates for an order are ordered
     * - key = deviceId → All telemetry from a device is ordered
     */
    @PostMapping("/send-with-key")
    public ResponseEntity<Map<String, Object>> sendMessageWithKey(
            @RequestParam String key,
            @RequestParam String message) {
        
        log.info("📨 REST API: Sending message with key: {} -> {}", key, message);
        
        // Send to Kafka with key
        messageProducer.sendMessageWithKey(key, message);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("key", key);
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("note", "Same key will always go to same partition!");
        
        return ResponseEntity.ok(response);
    }

    /**
     * SEND MESSAGE TO SPECIFIC PARTITION
     * 
     * Usage:
     *   curl -X POST "http://localhost:8080/api/send-to-partition?partition=0&message=Hello"
     * 
     * NOTE: This is rarely used in practice. Let Kafka decide partition!
     */
    @PostMapping("/send-to-partition")
    public ResponseEntity<Map<String, Object>> sendToPartition(
            @RequestParam int partition,
            @RequestParam String message) {
        
        log.info("📨 REST API: Sending to partition {}: {}", partition, message);
        
        if (partition < 0 || partition > 2) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Partition must be 0, 1, or 2 (we have 3 partitions)");
            return ResponseEntity.badRequest().body(error);
        }
        
        messageProducer.sendMessageToPartition(partition, null, message);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("partition", partition);
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }

    /**
     * SEND MULTIPLE MESSAGES (BATCH)
     * 
     * Usage:
     *   curl -X POST "http://localhost:8080/api/send-batch?count=10"
     * 
     * Great for testing:
     * - See how messages distribute across partitions
     * - Test consumer performance
     */
    @PostMapping("/send-batch")
    public ResponseEntity<Map<String, Object>> sendBatch(
            @RequestParam(defaultValue = "10") int count) {
        
        log.info("📨 REST API: Sending batch of {} messages", count);
        
        for (int i = 1; i <= count; i++) {
            String message = "Batch message #" + i + " at " + System.currentTimeMillis();
            messageProducer.sendMessage(message);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("messagesSent", count);
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("note", "Check console to see messages distributed across partitions");
        
        return ResponseEntity.ok(response);
    }

    /**
     * DEMO: Show how keys affect partition assignment
     * 
     * Usage:
     *   curl -X POST "http://localhost:8080/api/demo-keys"
     * 
     * Sends messages with different keys and shows which partition they go to
     */
    @PostMapping("/demo-keys")
    public ResponseEntity<Map<String, Object>> demoKeyPartitioning() {
        
        log.info("📨 REST API: Demonstrating key-based partitioning");
        
        // These will each go to potentially different partitions
        String[] keys = {"user-A", "user-B", "user-C", "user-A", "user-B", "user-A"};
        
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            String message = "Message " + (i + 1) + " for " + key;
            messageProducer.sendMessageWithKey(key, message);
            
            // Small delay to see logs clearly
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("messagesSent", keys.length);
        response.put("keys", keys);
        response.put("note", "Notice: same keys always go to same partition!");
        
        return ResponseEntity.ok(response);
    }

    /**
     * HEALTH CHECK
     * 
     * Usage:
     *   curl http://localhost:8080/api/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "kafka-simple");
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }
}
