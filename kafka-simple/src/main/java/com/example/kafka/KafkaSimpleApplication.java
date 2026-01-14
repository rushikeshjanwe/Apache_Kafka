package com.example.kafka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;




/**
 * =============================================================================
 * MAIN APPLICATION CLASS
 * =============================================================================
 * 
 * This is the entry point of your Spring Boot application.
 * 
 * @SpringBootApplication is a convenience annotation that combines:
 * 
 * 1. @Configuration 
 *    - Marks this class as a source of bean definitions
 * 
 * 2. @EnableAutoConfiguration
 *    - Tells Spring Boot to automatically configure beans based on:
 *      • Dependencies in pom.xml (it sees spring-kafka, so configures Kafka)
 *      • Properties in application.yml
 * 
 * 3. @ComponentScan
 *    - Tells Spring to scan this package and sub-packages for:
 *      • @Component, @Service, @Repository, @Controller
 *      • And automatically create beans from them
 * 
 * =============================================================================
 * WHAT HAPPENS WHEN YOU RUN THIS?
 * =============================================================================
 * 
 * 1. Spring Boot starts up
 * 2. Scans for components (@Controller, @Service, etc.)
 * 3. Reads application.yml configuration
 * 4. Auto-configures Kafka (because spring-kafka is in dependencies)
 * 5. Creates KafkaTemplate bean (for sending messages)
 * 6. Creates KafkaListenerContainerFactory (for @KafkaListener)
 * 7. Starts embedded Tomcat on port 8080
 * 8. Your app is ready!
 * 
 * =============================================================================
 */
@SpringBootApplication
public class KafkaSimpleApplication {

    public static void main(String[] args) {
        // This single line does ALL the magic!
        // It bootstraps the entire Spring application
        SpringApplication.run(KafkaSimpleApplication.class, args);
        
        System.out.println("\n");
        System.out.println("=================================================");
        System.out.println("🚀 Kafka Simple Application Started Successfully!");
        System.out.println("=================================================");
        System.out.println("📡 REST API:    http://localhost:8080");
        System.out.println("📊 Kafka UI:    http://localhost:8090");
        System.out.println("=================================================");
        System.out.println("\n📝 Try sending a message:");
        System.out.println("   curl -X POST \"http://localhost:8080/api/send?message=Hello%20Kafka\"");
        System.out.println("\n");
    }
}
