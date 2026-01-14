# 🚀 Kafka Simple - Learn Apache Kafka Basics

A dead-simple Spring Boot + Kafka project to understand the fundamentals.

---

## 📋 Table of Contents

1. [What You'll Learn](#what-youll-learn)
2. [Prerequisites](#prerequisites)
3. [Quick Start](#quick-start)
4. [API Endpoints](#api-endpoints)
5. [Experiments to Try](#experiments-to-try)
6. [Kafka Concepts Explained](#kafka-concepts-explained)
7. [Troubleshooting](#troubleshooting)

---

## 🎓 What You'll Learn

| Concept | Where to See It |
|---------|-----------------|
| **Broker** | Kafka server running in Docker |
| **Topic** | `messages` topic with 3 partitions |
| **Producer** | `MessageProducer.java` sends messages |
| **Consumer** | `MessageConsumer.java` receives messages |
| **Partition** | Watch messages distribute across 3 partitions |
| **Key** | Use `/api/send-with-key` to see key-based routing |
| **Offset** | See offset numbers in consumer logs |
| **Consumer Group** | Configured in `application.yml` |

---

## ⚙️ Prerequisites

- **Java 17+** 
  ```bash
  java -version
  ```

- **Maven**
  ```bash
  mvn -version
  ```

- **Docker & Docker Compose**
  ```bash
  docker --version
  docker-compose --version
  ```

---

## 🏃 Quick Start

### Step 1: Start Kafka

```bash
# Navigate to project directory
cd kafka-simple

# Start Kafka + Zookeeper + Kafka UI
docker-compose up -d

# Wait 30 seconds for Kafka to be ready
# Check status:
docker-compose ps
```

You should see:
```
NAME         STATUS
kafka        running (healthy)
kafka-ui     running
zookeeper    running (healthy)
```

### Step 2: Run Spring Boot Application

```bash
# In the kafka-simple directory
./mvnw spring-boot:run

# Or on Windows:
mvnw.cmd spring-boot:run
```

You should see:
```
🚀 Kafka Simple Application Started Successfully!
📡 REST API:    http://localhost:8080
📊 Kafka UI:    http://localhost:8090
```

### Step 3: Send Your First Message!

```bash
curl -X POST "http://localhost:8080/api/send?message=Hello%20Kafka"
```

### Step 4: Watch the Magic! ✨

In the Spring Boot console, you'll see:
```
📤 Sending message: Hello Kafka
✅ Message sent successfully!
   Topic: messages
   Partition: 1
   Offset: 0

╔════════════════════════════════════════════════════════════╗
║  📩 MESSAGE RECEIVED!                                       ║
╠════════════════════════════════════════════════════════════╣
║  Message: Hello Kafka
╚════════════════════════════════════════════════════════════╝
✅ Message processed successfully: Hello Kafka
```

### Step 5: Explore Kafka UI

Open http://localhost:8090 in your browser to:
- See topics
- View messages
- Monitor consumers

---

## 🌐 API Endpoints

### 1. Send Simple Message
```bash
curl -X POST "http://localhost:8080/api/send?message=Hello%20World"
```

### 2. Send Message with Key (Same key → Same partition)
```bash
curl -X POST "http://localhost:8080/api/send-with-key?key=user123&message=Hello"
```

### 3. Send to Specific Partition
```bash
curl -X POST "http://localhost:8080/api/send-to-partition?partition=0&message=Hello"
```

### 4. Send Batch (10 messages)
```bash
curl -X POST "http://localhost:8080/api/send-batch?count=10"
```

### 5. Demo Key Partitioning
```bash
curl -X POST "http://localhost:8080/api/demo-keys"
```

### 6. Health Check
```bash
curl http://localhost:8080/api/health
```

---

## 🧪 Experiments to Try

### Experiment 1: See Partition Distribution

```bash
# Send 20 messages without keys
curl -X POST "http://localhost:8080/api/send-batch?count=20"
```

Watch the logs - messages will be distributed across partitions 0, 1, 2.

---

### Experiment 2: Understand Keys

```bash
# Run the demo
curl -X POST "http://localhost:8080/api/demo-keys"
```

Notice in logs:
- `user-A` messages → always same partition
- `user-B` messages → always same partition (possibly different from A)
- `user-C` messages → always same partition

**This is how Kafka guarantees ordering for related messages!**

---

### Experiment 3: Verify Key Consistency

```bash
# Send multiple messages with same key
curl -X POST "http://localhost:8080/api/send-with-key?key=order-123&message=Order%20Created"
curl -X POST "http://localhost:8080/api/send-with-key?key=order-123&message=Order%20Paid"
curl -X POST "http://localhost:8080/api/send-with-key?key=order-123&message=Order%20Shipped"
```

All three messages will go to the **same partition** → processed in order!

---

### Experiment 4: Check Kafka UI

1. Open http://localhost:8090
2. Click on "Topics" → "messages"
3. Click on "Messages" tab
4. See all your messages with partition and offset info!

---

### Experiment 5: Consumer Groups (Advanced)

1. Stop the application (`Ctrl+C`)
2. In `MessageConsumer.java`, change the groupId:
   ```java
   @KafkaListener(topics = "messages", groupId = "new-consumer-group")
   ```
3. Restart the application
4. Send a message → It will receive ALL old messages!

**Why?** New consumer group has no committed offsets, starts from `earliest`.

---

## 📚 Kafka Concepts Explained

### 🔹 Topic
A topic is like a **folder** or **channel** for messages.
- Our topic: `messages`
- Messages are stored in topics
- Producers send to topics, Consumers read from topics

### 🔹 Partition
A topic is split into **partitions** for scalability.
```
Topic: messages
├── Partition 0: [msg0, msg3, msg6...]
├── Partition 1: [msg1, msg4, msg7...]
└── Partition 2: [msg2, msg5, msg8...]
```
- More partitions = More parallelism
- Messages in ONE partition are ordered
- Messages ACROSS partitions are NOT ordered

### 🔹 Offset
Each message in a partition has a unique **offset** (position number).
```
Partition 0: offset 0, 1, 2, 3, 4, 5...
```
- Consumers track their position using offsets
- "I've read up to offset 5" = Committed offset

### 🔹 Producer
Application that **sends** messages to Kafka.
- Uses `KafkaTemplate` in Spring
- Decides which partition (via key hash or round-robin)

### 🔹 Consumer
Application that **reads** messages from Kafka.
- Uses `@KafkaListener` in Spring
- Part of a Consumer Group

### 🔹 Consumer Group
Multiple consumers working together.
```
Consumer Group: my-first-consumer-group
├── Consumer A → Partition 0, 1
└── Consumer B → Partition 2
```
- Each partition assigned to ONE consumer in group
- If consumer dies, partitions reassigned
- Different groups get ALL messages (like pub-sub)

### 🔹 Key
Optional identifier for messages.
- Same key → Same partition → Ordered processing
- No key → Round-robin distribution

---

## 🔧 Troubleshooting

### Kafka Not Starting
```bash
# Check logs
docker-compose logs kafka

# Restart everything
docker-compose down
docker-compose up -d
```

### Application Can't Connect
```
Error: Connection to node -1 could not be established
```
- Wait 30 seconds after `docker-compose up`
- Check Kafka is healthy: `docker-compose ps`

### Consumer Not Receiving Messages
- Check consumer group ID in logs
- Check `auto-offset-reset: earliest` in application.yml
- Verify topic exists in Kafka UI

### Port Already in Use
```bash
# Check what's using port 8080
lsof -i :8080

# Change port in application.yml:
server:
  port: 8081
```

---

## 📁 Project Structure

```
kafka-simple/
├── docker-compose.yml              # Kafka + Zookeeper setup
├── pom.xml                         # Maven dependencies
├── src/main/java/com/example/kafka/
│   ├── KafkaSimpleApplication.java    # Main class
│   ├── config/
│   │   └── KafkaTopicConfig.java      # Topic creation
│   ├── controller/
│   │   └── MessageController.java     # REST API
│   ├── producer/
│   │   └── MessageProducer.java       # Sends messages
│   └── consumer/
│       └── MessageConsumer.java       # Receives messages
└── src/main/resources/
    └── application.yml                # Configuration
```

---

## 🛑 Stopping Everything

```bash
# Stop Spring Boot
Ctrl+C

# Stop Kafka
docker-compose down

# Stop Kafka and remove data
docker-compose down -v
```

---

## ➡️ Next Steps

Once you understand this, you're ready for the **E-Commerce Order System** with:
- Multiple topics
- Multiple consumer groups
- JSON serialization
- Manual offset commits
- Dead Letter Queues
- And more!

---

**Happy Learning! 🎉**
