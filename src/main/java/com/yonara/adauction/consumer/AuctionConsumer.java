package com.yonara.adauction.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yonara.adauction.model.AdBidEvent;
import com.yonara.adauction.model.AuctionResult;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;

@Component
public class AuctionConsumer {

    private final ObjectMapper mapper = new ObjectMapper();

    public void startConsuming() throws Exception {

        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("group.id", "auction-engine");
        props.put("key.deserializer",
            "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer",
            "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("auto.offset.reset", "earliest");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("ad-bids"));

        System.out.println("Consumer started. Listening for bid events...");

        Map<String, List<AdBidEvent>> bidsBySlot = new HashMap<>();

        while (true) {
            ConsumerRecords<String, String> records =
                consumer.poll(Duration.ofMillis(1000));

            for (ConsumerRecord<String, String> record : records) {
                AdBidEvent event = mapper.readValue(record.value(),
                    AdBidEvent.class);
                bidsBySlot
                    .computeIfAbsent(event.getAdSlot(),
                        k -> new ArrayList<>())
                    .add(event);
            }

            bidsBySlot.forEach((slot, bids) -> {
                AdBidEvent winner = bids.stream()
                    .max(Comparator.comparingDouble(
                        AdBidEvent::getBidAmount))
                    .orElse(null);

                if (winner != null) {
                    AuctionResult result = new AuctionResult(
                        slot,
                        winner.getAdvertiserId(),
                        winner.getBidAmount(),
                        System.currentTimeMillis(),
                        winner.getUserProfile()
                    );
                    System.out.println("Winner: " + result.getWinnerAdvertiserId()
                        + " for slot " + result.getAdSlot()
                        + " with bid $" + result.getWinningBid());
                }
            });

            bidsBySlot.clear();
        }
    }
}