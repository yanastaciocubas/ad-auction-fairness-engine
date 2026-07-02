# ad-auction-fairness-engine

I studied Computer Science and Public Health at Columbia, and one of my courses (Ethical AI) assigned us Latanya Sweeney's 2013 paper showing that search engines were serving arrest record ads disproportionately to people with Black sounding names, even when those people had no criminal record. That stayed with me.

Around the same time I was learning how programmatic advertising works under the hood. Real time bidding, Kafka pipelines, auction engines. And I kept thinking: if the bidding system has no fairness layer built in, discrimination does not need to be intentional. It just emerges from the optimization.

So I built this. It is a real time ad auction pipeline in Java and Kafka that audits every auction decision for demographic fairness. The goal was to understand both sides: how the system works technically, and where bias quietly enters.

---

## How it works

Simulated advertisers submit bids for ad slots, tagged with an ad category and a simulated user profile (age group, gender, income level). A Kafka consumer batches bids into one second windows, picks the second-price winner per slot, and persists every result to PostgreSQL. The fairness auditor then runs over that window's results and checks the distribution of premium vs. predatory ads across income groups. Anything that crosses the fairness threshold gets persisted as an alert, published to a Kafka topic, and shows up immediately through the API.

```
Bid Events (Java Producer)
        ↓
Kafka: ad-bids  [3 partitions, keyed by slot]
        ↓
Auction Consumer -> picks winner -> PostgreSQL (auction_results)
        ↓
Fairness Auditor -> checks demographic parity
                 -> checks predatory ad targeting
        ↓
Fairness Alert Service -> PostgreSQL (fairness_alerts)
                        -> Kafka: fairness-alerts
        ↓
Spring Boot API  ->  /api/fairness/report
                 ->  /api/fairness/demographic-parity/{adCategory}
                 ->  /api/fairness/predatory-ad-distribution
                 ->  /api/fairness/flagged-advertisers
                 ->  /api/analytics/top-advertisers
        ↓
Deployed on AWS EC2
```

---

## Stack

Java 17, Apache Kafka 3.6, Spring Boot 3, PostgreSQL, Docker, AWS EC2

---

## Getting it running

You need Docker Desktop installed. Once Docker is running, one command starts everything:

```
docker-compose up -d
```

Then create the Kafka topics:

```
kafka-topics --create --topic ad-bids \
  --bootstrap-server localhost:9092 \
  --partitions 3 --replication-factor 1

kafka-topics --create --topic fairness-alerts \
  --bootstrap-server localhost:9092 \
  --partitions 1 --replication-factor 1
```

Set up the database:

```
psql -U postgres -f sql/schema.sql
```

Build and run the app. This starts the REST API and the auction consumer (which begins listening for bid events) in one process:

```
mvn clean package
java -jar target/ad-auction-fairness-engine.jar
```

In a second terminal, run the load generator to simulate bid traffic:

```
mvn compile exec:java -Dexec.mainClass="com.yonara.adauction.producer.AdBidProducer"
```

Watch the consumer logs for winners and fairness alerts, or query the API directly once results start landing in Postgres.

---

## Project structure

```
src/main/java/com/yonara/adauction/
├── model/
│   ├── AdBidEvent.java
│   ├── AuctionResult.java
│   ├── FairnessAlert.java
│   └── UserProfile.java
├── producer/
│   └── AdBidProducer.java
├── consumer/
│   ├── AuctionConsumer.java
│   └── PipelineRunner.java
├── fairness/
│   ├── FairnessAuditor.java
│   └── FairnessAlertService.java
├── repository/
│   ├── AuctionResultRepository.java
│   └── FairnessAlertRepository.java
└── api/
    └── AnalyticsController.java
```

`FairnessAuditor` is intentionally a pure function with no database or Kafka dependency: given a window of auction results, it returns a list of alerts. All persistence and publishing lives in `FairnessAlertService`. That split is what makes the fairness logic cheap to unit test (see `FairnessAuditorTest`) without spinning up infrastructure.

---

## The fairness layer

Users in the simulation have demographic attributes: age group, gender, income level. Bids also carry an ad category, including two modeled as predatory: `payday-loan` and `criminal-record-search`. The load generator weights those categories toward low-income users (35% of low-income bids vs. 3% of high-income bids), which is what gives the auditor something real to catch, rather than auditing uniformly random data.

The `FairnessAuditor` runs two checks per ad category, per one-second window:

**Demographic parity.** Are average winning bids for a given ad category roughly equal across income groups? If the gap between the highest and lowest group exceeds 15%, it raises a flag.

**Predatory ad targeting.** For `payday-loan` and `criminal-record-search` specifically: are low-income users winning (seeing) these ads at more than 1.5x the rate of high-income users? This is the Sweeney finding, reproduced in a live system.

Results come through the API:

```
GET /api/fairness/report
GET /api/fairness/demographic-parity/{adCategory}
GET /api/fairness/predatory-ad-distribution
GET /api/fairness/flagged-advertisers
```

---

## Testing

```
mvn test
```

`FairnessAuditorTest` covers both checks directly: that a large bid gap across income groups gets flagged, that balanced distributions don't, and that the predatory-targeting check only fires for the two categories it's meant to cover. Because the auditor has no I/O dependencies, these run in milliseconds with no Postgres or Kafka required.

---

## What I ran into

Kafka partition assignment was the first real headache. I initially keyed messages by advertiser ID, which meant bids for the same ad slot were scattered across partitions and the auction logic was comparing bids that were never in the same window. Switching the partition key to ad slot fixed it, but it took me a while to understand why that mattered.

The bigger lesson came later, when I went back through the code to add real tests. I found the consumer was picking auction winners and printing them to the console, but never actually saving them to Postgres, and the fairness auditor was never being called at all. The README described a working pipeline that, in practice, dead-ended after the auction logic. I rewired the consumer to persist results and invoke the auditor, split the auditor into pure logic versus a separate persistence/publishing service so it could actually be unit tested, and added ad categories to the bid data so the predatory targeting check had something real to evaluate instead of random noise. It was a useful reminder to verify a system end to end instead of trusting that each piece working in isolation means the whole pipeline works.

The fairness thresholds are also somewhat arbitrary. 15% disparity and a 1.5x ratio feel reasonable but I do not have a principled justification for those numbers. Turns out this is an open question in the actual research too, which I found humbling.

---

## What is next

Add a frontend dashboard to visualize fairness metrics in real time. Experiment with first price vs second price auction mechanisms and see if fairness outcomes change. Replace simulated demographics with a more realistic synthetic dataset.

---

## References

Sweeney, L. (2013). Discrimination in Online Ad Delivery. ACM Queue.

Buolamwini, J., Gebru, T. (2018). Gender Shades. FAACT.

Salleb-Aouissi, A. (2025). Foundations of Ethical and Responsible AI. Columbia University COMS 4710.