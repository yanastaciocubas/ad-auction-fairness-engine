package com.yonara.adauction.fairness;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yonara.adauction.model.FairnessAlert;
import com.yonara.adauction.repository.FairnessAlertRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Handles the side effects of a fairness check: writing the alert to
 * Postgres so it shows up in /api/fairness/report, and publishing it to
 * the fairness-alerts Kafka topic so downstream consumers (a dashboard,
 * an alerting service) could react to it in real time.
 */
@Service
public class FairnessAlertService {

    private static final String FAIRNESS_ALERTS_TOPIC = "fairness-alerts";

    private final FairnessAlertRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    public FairnessAlertService(FairnessAlertRepository repository,
                                 KafkaTemplate<String, String> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    public void handle(List<FairnessAlert> alerts) {
        for (FairnessAlert alert : alerts) {
            repository.save(alert);
            publish(alert);
            System.out.println("FAIRNESS ALERT: " + alert);
        }
    }

    private void publish(FairnessAlert alert) {
        try {
            String json = mapper.writeValueAsString(alert);
            kafkaTemplate.send(FAIRNESS_ALERTS_TOPIC, alert.getAdCategory(), json);
        } catch (Exception e) {
            // Publishing to Kafka is best-effort: the alert is already
            // persisted to Postgres, so we don't want a broker hiccup
            // here to look like the audit itself failed.
            System.err.println("Failed to publish fairness alert to Kafka: " + e.getMessage());
        }
    }
}