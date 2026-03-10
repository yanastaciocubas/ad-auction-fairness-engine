# ad-auction-fairness-engine

I'm studying Computer Science and Public Health at Columbia, and one of my courses (Ethical AI) assigned us Latanya Sweeney's 2013 paper showing that search engines were serving arrest record ads disproportionately to people with Black sounding names, even when those people had no criminal record. That stayed with me.

Around the same time I was learning how programmatic advertising works under the hood. Real time bidding, Kafka pipelines, auction engines. And I kept thinking: if the bidding system has no fairness layer built in, discrimination does not need to be intentional. It just emerges from the optimization.

So I built this. It is a real time ad auction pipeline in Java and Kafka that audits every auction decision for demographic fairness. The goal was to understand both sides: how the system works technically, and where bias quietly enters.

---

## How it works

Simulated advertisers submit bids for ad slots. A Kafka consumer picks winners using second price auction logic. Then a second layer, the fairness auditor, checks whether the distribution of premium vs predatory ads across user demographics violates fairness criteria from my course (demographic parity, equal opportunity). Anything suspicious gets flagged to a separate Kafka topic and logged.

```
Bid Events (Java Producer)
        ↓
Kafka: ad-bids  [3 partitions, keyed by slot]
        ↓
Auction Consumer -> picks winner -> PostgreSQL
        ↓
Fairness Auditor -> checks demographic parity
                 -> flags predatory ad targeting
        ↓
Kafka: fairness-alerts
        ↓
Spring Boot API  ->  /api/fairness/report
                 ->  /api/analytics/top-advertisers
                 ->  /api/fairness/predatory-ad-distribution
        ↓
Deployed on AWS EC2
```

---

## Stack

Java 17, Apache Kafka 3.6, Spring Boot 3, PostgreSQL, Docker, AWS EC2

---

## Getting it running

You need Docker Desktop installed. Once Docker is running, one command starts everything:

```bash
docker-compose up -d
```

Then create the Kafka topics:

```bash
kafka-topics --create --topic ad-bids \
  --bootstrap-server localhost:9092 \
  --partitions 3 --replication-factor 1

kafka-topics --create --topic auction-results \
  --bootstrap-server localhost:9092 \
  --partitions 3 --replication-factor 1

kafka-topics --create --topic fairness-alerts \
  --bootstrap-server localhost:9092 \
  --partitions 1 --replication-factor 1
```

Set up the database:

```bash
psql -U postgres -f sql/schema.sql
```

Build and run:

```bash
mvn clean package
java -jar target/ad-auction-fairness-engine.jar
```

---

## Project structure

```
src/main/java/com/yonara/adauction/
├── model/
│   ├── AdBidEvent.java
│   ├── AuctionResult.java
│   └── UserProfile.java
├── producer/
│   └── AdBidProducer.java
├── consumer/
│   └── AuctionConsumer.java
├── fairness/
│   └── FairnessAuditor.java
├── repository/
│   └── AuctionResultRepository.java
└── api/
    └── AnalyticsController.java
```

---

## The fairness layer

Users in the simulation have demographic attributes: age group, gender, income level. The FairnessAuditor runs two checks after every auction window.

**Demographic parity.** Are premium ads reaching all income groups at roughly equal rates? If the gap between the highest and lowest group exceeds 15%, it raises a flag.

**Predatory ad targeting.** Are payday loan and criminal record search ads concentrating on specific demographics even when bid amounts are equal across groups? This is the Sweeney finding, basically reproduced in a live system.

Results come through the API:

```
GET /api/fairness/report
GET /api/fairness/demographic-parity/{adCategory}
GET /api/fairness/predatory-ad-distribution
GET /api/fairness/flagged-advertisers
```

---

## What I ran into

Kafka partition assignment was the first real headache. I initially keyed messages by advertiser ID, which meant bids for the same ad slot were scattered across partitions and the auction logic was comparing bids that were never in the same window. Switching the partition key to ad slot fixed it, but it took me a while to understand why that mattered.

The fairness thresholds are also somewhat arbitrary. 15% disparity feels reasonable but I do not have a principled justification for that number. Turns out this is an open question in the actual research too, which I found humbling.

---

## What is next

Add a frontend dashboard to visualize fairness metrics in real time. Experiment with first price vs second price auction mechanisms and see if fairness outcomes change. Replace simulated demographics with a more realistic synthetic dataset.

---

## References

Sweeney, L. (2013). Discrimination in Online Ad Delivery. ACM Queue.

Buolamwini, J., Gebru, T. (2018). Gender Shades. FAACT.

Salleb-Aouissi, A. (2025). Foundations of Ethical and Responsible AI. Columbia University COMS 4710.