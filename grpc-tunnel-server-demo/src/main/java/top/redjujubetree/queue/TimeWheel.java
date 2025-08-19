package top.redjujubetree.queue;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TimeWheel {
    private final int ticksPerWheel;
    private final long tickDurationMillis;
    private final List<Set<QueueMessage>> wheel;
    private final AtomicInteger currentTick = new AtomicInteger(0);
    private final ScheduledExecutorService scheduler;
    private final MessageRepublisher messageRepublisher;

    public TimeWheel(int ticksPerWheel, long tickDurationMillis, MessageRepublisher messageRepublisher) {
        this.ticksPerWheel = ticksPerWheel;
        this.tickDurationMillis = tickDurationMillis;
        this.messageRepublisher = messageRepublisher;
        this.wheel = new ArrayList<>(ticksPerWheel);
        for (int i = 0; i < ticksPerWheel; i++) {
            wheel.add(Collections.synchronizedSet(new HashSet<>()));
        }
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::tick, tickDurationMillis, tickDurationMillis, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    private void tick() {
        int tickIndex = currentTick.getAndUpdate(i -> (i + 1) % ticksPerWheel);
        Set<QueueMessage> queueMessages = wheel.get(tickIndex);
        synchronized (queueMessages) {
            for (QueueMessage msg : queueMessages) {
                messageRepublisher.republish(msg);
            }
            queueMessages.clear();
        }
    }

    public void add(QueueMessage msg, long delayMillis) {
        if (delayMillis < 0) delayMillis = 0;
        int ticks = (int)(delayMillis / tickDurationMillis);
        int slot = (currentTick.get() + ticks) % ticksPerWheel;
        wheel.get(slot).add(msg);
    }
}
