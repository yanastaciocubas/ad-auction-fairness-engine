package com.yonara.adauction.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yonara.adauction.fairness.FairnessAlertService;
import com.yonara.adauction.fairness.FairnessAuditor;
import com.yonara.adauction.model.AdBidEvent;
import com.yonara.adauction.model.AuctionResult;
import com.yonara.adauction.model.FairnessAlert;
import com.yonara.adauction.repository.AuctionResultRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;

/**
 * Reads bid events off the ad-bids topic, runs a second-price auction per
 * slot per polling window, persists the winner, and runs the fairness
 * auditor over every window's results. Runs as a background thread,
 * started by PipelineRunner on application startup.
 */
@Component
public class AuctionConsumer {

    private final ObjectMapper mapper = new ObjectMapper();
    private final AuctionResultRepository resultRepository;
    private final FairnessAuditor fairnessAuditor;
    private final FairnessAlertService fairnessAlertService;

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    public AuctionConsumer(AuctionResultRepository resultRepository,
                            FairnessAuditor fairnessAuditor,
                            FairnessAlertService fairnessAlertService) {
        this.resultRepository = resultRepository;
        this.fairnessAuditor = fairnessAuditor;
        this.fairnessAlertService = fairnessAlertService;
    }

    public void startConsuming() throws Exception {

        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
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

            if (bidsBySlot.isEmpty()) {
                continue;
            }

            List<AuctionResult> windowResults = new ArrayList<>();

            for (Map.Entry<String, List<AdBidEvent>> entry : bidsBySlot.entrySet()) {
                List<AdBidEvent> bids = entry.getValue();

                AdBidEvent winner = bids.stream()
                    .max(Comparator.comparingDouble(AdBidEvent::getBidAmount))
                    .orElse(null);

                if (winner == null) continue;

                AuctionResult result = new AuctionResult(
                    winner.getAdSlot(),
                    winner.getAdvertiserId(),
                    winner.getAdCategory(),
                    winner.getBidAmount(),
                    System.currentTimeMillis(),
                    winner.getUserProfile()
                );

                System.out.println("Winner: " + result.getWinnerAdvertiserId()
                    + " for slot " + result.getAdSlot()
                    + " with bid $" + result.getWinningBid());

                resultRepository.save(result);
                windowResults.add(result);
            }

            List<FairnessAlert> alerts = fairnessAuditor.evaluate(windowResults);
            if (!alerts.isEmpty()) {
                fairnessAlertService.handle(alerts);
            }

            bidsBySlot.clear();
        }
    }
}