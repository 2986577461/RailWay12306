package com.xiaoyan.railway.order;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class OrderIdGenerator {
    private static final long EPOCH = 1_704_067_200_000L;
    private final AtomicInteger sequence = new AtomicInteger();
    private final int workerId;
    private volatile long lastMillis = -1L;

    public OrderIdGenerator(@Value("${railway.worker-id:1}") int workerId) {
        if (workerId < 0 || workerId > 31) throw new IllegalArgumentException("worker-id must be between 0 and 31");
        this.workerId = workerId;
    }

    public synchronized long nextId() {
        long now = System.currentTimeMillis();
        if (now != lastMillis) {
            lastMillis = now;
            sequence.set(0);
        }
        int value = sequence.getAndIncrement();
        if (value > 4095) throw new IllegalStateException("order id sequence exhausted for current millisecond");
        return ((now - EPOCH) << 17) | ((long) workerId << 12) | value;
    }
}