package com.yonara.adauction.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yonara.adauction.model.AdBidEvent;
import com.yonara.adauction.model.UserProfile;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;
import java.util.Random;
import java.util.UUID;

/**
 * Standalone load generator. Simulates bid traffic with a realistic bias:
 * predatory ad categories (payday loans, criminal-record-search) are
 * targeted at low-income users far more often than high-income users,
 * at comparable bid amounts. That's what reproduces the Sweeney (2013)
 * pattern for FairnessAuditor to actually detect, instead of auditing
 * uniformly random data where no disparity would ever appear.
 */
public class AdBidProducer {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Random random = new Random();

    private static final String[] advertisers = {"nike", "apple", "amazon", "spotify"};
    private static final String[] slots = {"homepage-banner", "sidebar", "feed-card"};
    private static final String[] ageGroups = {"18-34", "35-54", "55+"};
    private static final String[] genders = {"M", "F", "NB"};
    private static final String[] incomeLevels = {"low", "medium", "high"};

    private static final String[] normalCategories =
        {"retail", "tech", "travel", "finance-prime"};
    private static final String[] predatoryCategories =
        {"payday-loan", "criminal-record-search"};

    public static void main(String[] args) throws Exception {

        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.serializer",
            "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer",
            "org.apache.kafka.common.serialization.StringSerializer");

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);

        System.out.println("Producer started. Sending bid events...");

        for (int i = 0; i < 10000; i++) {

            String userId = "user-" + random.nextInt(1000);
            String incomeLevel = incomeLevels[random.nextInt(incomeLevels.length)];

            UserProfile profile = new UserProfile(
                userId,
                ageGroups[random.nextInt(ageGroups.length)],
                genders[random.nextInt(genders.length)],
                incomeLevel
            );

            double bidAmount;
            if (incomeLevel.equals("low")) {
                bidAmount = 0.01 + random.nextDouble() * 4.50;
            } else {
                bidAmount = 0.01 + random.nextDouble() * 5.00;
            }

            String adCategory = pickAdCategory(incomeLevel);

            AdBidEvent event = new AdBidEvent(
                UUID.randomUUID().toString(),
                advertisers[random.nextInt(advertisers.length)],
                slots[random.nextInt(slots.length)],
                adCategory,
                bidAmount,
                System.currentTimeMillis(),
                userId,
                profile
            );

            String json = mapper.writeValueAsString(event);
            producer.send(new ProducerRecord<>("ad-bids", event.getAdSlot(), json));

            Thread.sleep(10);
        }

        producer.close();
        System.out.println("Producer finished.");
    }

    /**
     * Predatory categories are weighted heavily toward low-income users
     * and rarely shown to high-income users, mirroring the disproportionate
     * targeting Sweeney documented in real ad-delivery systems.
     */
    private static String pickAdCategory(String incomeLevel) {
        double predatoryChance = switch (incomeLevel) {
            case "low" -> 0.35;
            case "medium" -> 0.12;
            default -> 0.03; // high
        };

        if (random.nextDouble() < predatoryChance) {
            return predatoryCategories[random.nextInt(predatoryCategories.length)];
        }
        return normalCategories[random.nextInt(normalCategories.length)];
    }
}