package com.sentinel;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class EventQueueProcessor {
    private final BlockingQueue<RingEvent> eventQueue;

    public EventQueueProcessor(int capacity) {
        // Thread-safe bounded queue enforcing [D1] DoS mitigation
        this.eventQueue = new ArrayBlockingQueue<>(capacity);
    }

    public boolean offerEvent(RingEvent event) {
        boolean accepted = eventQueue.offer(event);
        if (!accepted) {
            System.err.println("[RATE LIMIT / DOS ALERT] Buffer full! Dropped event: " + event.eventId());
        } else {
            System.out.println("[INBOUND QUEUE] Enqueued event: " + event.eventId() + " | Active size: " + eventQueue.size());
        }
        return accepted;
    }

    public RingEvent takeEvent() throws InterruptedException {
        return eventQueue.take();
    }

    public int getQueueSize() {
        return eventQueue.size();
    }
}