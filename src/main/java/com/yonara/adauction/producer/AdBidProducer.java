package com.yonara.adauction.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yonara.adauction.model.AdBidEvent;
import com.yonara.adauction.model.UserProfile;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;

import java.util.Properties;
import java.util.Random;
import java.util.UUID;

@Component
public class AdBidProducer {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Random random = new Random();

    private final String[] advertisers = {"nike", "apple", "amazon", "spotify"};
    private final String[] slots = {"homepage-banner", "sidebar", "feed-card"};
    private final String[] ageGroups = {"18-34", "35-54", "55+"};
    private final String[] genders = {"M", "F", "NB"};
    private final String[] incomeLevels = {"low", "medium", "high"};

    public void startProducing() throws Exception {

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

            AdBidEvent event = new AdBidEvent(
                UUID.randomUUID().toString(),
                advertisers[random.nextInt(advertisers.length)],
                slots[random.nextInt(slots.length)],
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
}