package com.yonara.adauction.consumer;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * AuctionConsumer.startConsuming() is a blocking infinite loop, so it
 * can't run on Spring's main thread without blocking the REST API from
 * ever starting up. This kicks it off on a daemon thread instead, so
 * `java -jar` boots the API and starts consuming bid events in one step.
 */
@Component
public class PipelineRunner implements CommandLineRunner {

    private final AuctionConsumer auctionConsumer;

    public PipelineRunner(AuctionConsumer auctionConsumer) {
        this.auctionConsumer = auctionConsumer;
    }

    @Override
    public void run(String... args) {
        Thread consumerThread = new Thread(() -> {
            try {
                auctionConsumer.startConsuming();
            } catch (Exception e) {
                System.err.println("Auction consumer stopped unexpectedly: " + e.getMessage());
            }
        }, "auction-consumer");

        consumerThread.setDaemon(true);
        consumerThread.start();
    }
}