package com.toonflow.agent.core;

import java.util.concurrent.ConcurrentLinkedQueue;

public class MessageQueue {

    private final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();

    public void enqueue(String message) {
        queue.offer(message);
    }

    public String dequeue() {
        return queue.poll();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public void clear() {
        queue.clear();
    }
}
