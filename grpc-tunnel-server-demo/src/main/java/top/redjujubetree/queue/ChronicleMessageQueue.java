package top.redjujubetree.queue;

import lombok.Getter;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import net.openhft.chronicle.wire.DocumentContext;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * ChronicleMessageQueue
 *
 * <pre>
 * Applicable scenarios:
 *   1. High throughput message processing
 *   2. Business with idempotency requirements
 *   3. Can tolerate duplicate processing
 *   4. Extreme performance pursuit
 *   5. Memory-sensitive environments
 * </pre>
 */
public class ChronicleMessageQueue {

    private static Logger log = LoggerFactory.getLogger("ChronicleMessageQueue");
    private final ChronicleQueue queue;
    private final ExcerptAppender appender;
    private final Map<String, ExcerptTailer> tailerMap = new ConcurrentHashMap<>();
    private final Map<String, List<ConsumerInfo>> subscriptions = new ConcurrentHashMap<>();
    private final ExecutorService executor;
    private final TimeWheel timeWheel;

    private final AtomicBoolean isRunning;
    private final AtomicLong publishedCount = new AtomicLong(0);
    private final AtomicLong processedCount = new AtomicLong(0);
    private final AtomicLong failedCount = new AtomicLong(0);

    @Getter
	private static class ConsumerInfo {
        private final String topic;
        private final String groupId;
        private final TaskHandler handler;
        private final boolean enableBroadcast;

        public ConsumerInfo(String topic, String groupId, TaskHandler handler, boolean enableBroadcast) {
            this.topic = topic;
            this.groupId = groupId;
            this.handler = handler;
            this.enableBroadcast = enableBroadcast;
        }
	}

    public ChronicleMessageQueue(String path, int threadCount, int ticksPerWheel, long tickDurationMillis) {
        this.queue = SingleChronicleQueueBuilder.binary(path).build();
        this.appender = queue.acquireAppender();

        this.executor = new ThreadPoolExecutor(threadCount, threadCount, 60L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1000), r -> {;
            Thread t = new Thread(r, "Consumer-Worker");
            // Consumer worker threads are essentially background threads that process messages.
            // * They are not part of the main application logic.
            // * They should be terminated when the application is about to shut down.
            // * They should not prevent the application from shutting down normally.
            t.setDaemon(true);
            return t;
        },new ThreadPoolExecutor.CallerRunsPolicy());

        this.timeWheel = new TimeWheel(ticksPerWheel, tickDurationMillis, this::publish);
        
        timeWheel.start();
        isRunning = new AtomicBoolean(true);
        log.info("MessageQueue started with path: {}, threadCount: {}, ticksPerWheel: {}, tickDurationMillis: {}");
    }

    /**
     * subscribe to a topic with a consumer group and handler
     * @param topic topic name
     * @param consumerGroup consumer group
     * @param handler message handler
     */
    public void subscribe(String topic, ConsumerGroup consumerGroup, TaskHandler handler) {
        ConsumerInfo consumerInfo = new ConsumerInfo(topic, consumerGroup.getGroupId(), 
                                                   handler, consumerGroup.isEnableBroadcast());
        
        subscriptions.computeIfAbsent(topic, k -> new ArrayList<>()).add(consumerInfo);
        
        // to ensure each consumer group has its own tailer
        String tailerId = topic + "-" + consumerGroup.getGroupId();
        if (!tailerMap.containsKey(tailerId)) {
            ExcerptTailer tailer = queue.createTailer(tailerId);
            tailerMap.put(tailerId, tailer);
            startReaderForConsumer(consumerInfo, tailer);
        }
        log.info("Consumer group '{}' subscribed to topic: {} (Broadcast mode: {})",
                consumerGroup.getGroupId(), topic, consumerGroup.isEnableBroadcast());
    }

    /**
     * publish a message to the queue
     */
    public void publish(QueueMessage msg) {
        long now = System.currentTimeMillis();
        if (now < msg.getNotBefore()) {
            // delay message processing
            long delay = msg.getNotBefore() - now;
            timeWheel.add(msg, delay);
            return;
        }

        appender.writeDocument(w -> w.write("msg").object(msg));
        publishedCount.incrementAndGet();
        if (publishedCount.get() % 1000 == 0) {
            log.info("Already published {} messages", publishedCount.get());
        }
    }

    /**
     * batch publish messages to the queue
     */
    public void publishBatch(List<QueueMessage> messages) {
        for (QueueMessage msg : messages) {
            publish(msg);
        }
    }

    /**
     * Every consumer group has its own tailer and reading thread.
     */
    private void startReaderForConsumer(ConsumerInfo consumerInfo, ExcerptTailer tailer) {
        Thread readerThread = new Thread(() -> {
            log.info("starting consumer thread for group: {} on topic: {}", consumerInfo.getGroupId(), consumerInfo.getTopic());
            long timeWait = 50; // Initial wait time in milliseconds
            while (isRunning.get()) {
                try (DocumentContext dc = tailer.readingDocument()) {

                    if (!dc.isPresent()) {
                        timeWait += 50; // Increase wait time if no message is present
                        LockSupport.parkNanos(Math.min(timeWait, 1000));
                        if (Thread.currentThread().isInterrupted()) {
                            // Respond to interrupt signal
                            Thread.interrupted();
                        }
                        continue;
                    }

                    QueueMessage msg = dc.wire().read("msg").object(QueueMessage.class);
                    
                    if (!consumerInfo.getTopic().equals(msg.getTopic())) {
                        continue;
                    }

                    if (Strings.isNotEmpty(msg.getConsumerGroup()) && !Objects.equals(msg.getConsumerGroup(), consumerInfo.getGroupId())) {
                        continue;
                    }

                    long now = System.currentTimeMillis();
                    if (now >= msg.getNotBefore()) {
                        processMessage(msg, consumerInfo);
                    } else {
                        // delay message processing
                        long delay = msg.getNotBefore() - now;
                        timeWheel.add(msg, delay);
                    }
                    
                } catch (Exception e) {
                    log.error("Consumer {} reading error:", consumerInfo.getGroupId(),e);
                }
            }
            log.info("Consumer thread for group {} on topic {} has stopped.", consumerInfo.getGroupId(), consumerInfo.getTopic());
        }, "Reader-" + consumerInfo.getTopic() + "-" + consumerInfo.getGroupId());
        
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void processMessage(QueueMessage msg, ConsumerInfo consumerInfo) {
        executor.submit(() -> {
            try {
                consumerInfo.getHandler().handle(msg);
                processedCount.incrementAndGet();
                
                if (processedCount.get() % 1000 == 0) {
                    log.info("Consumer [{}] has processed {} messages", consumerInfo.getGroupId(), processedCount.get());
                }
            } catch (Exception e) {
                failedCount.incrementAndGet();
                handleRetry(msg, consumerInfo, e);
            }
        });
    }

    private void handleRetry(QueueMessage msg, ConsumerInfo consumerInfo, Exception error) {
        if (msg.getRetryCount() < msg.getMaxRetries()) {
            msg.setRetryCount(msg.getRetryCount() + 1);
            long notBefore = System.currentTimeMillis() + msg.getRetryDelayMillis() * msg.getRetryCount() * msg.getRetryCount();
            msg.setNotBefore(notBefore);

            log.info("Retrying message [{}] for group [{}] - Attempt {}/{}", msg.getId(),consumerInfo.getGroupId(), msg.getRetryCount(), msg.getMaxRetries());

            publish(msg);
        } else {
            consumerInfo.getHandler().onError(msg,consumerInfo.getGroupId(),error);
            failedCount.incrementAndGet();
        }
    }

    public PerformanceStats getPerformanceStats() {
        return new PerformanceStats(
            publishedCount.get(),
            processedCount.get(),
            failedCount.get(),
            subscriptions.size(),
            tailerMap.size()
        );
    }

    public Map<String, Integer> getSubscriptionStats() {
        Map<String, Integer> stats = new HashMap<>();
        subscriptions.forEach((topic, consumers) -> 
            stats.put(topic, consumers.size()));
        return stats;
    }

    /**
     * Get the number of consumers for a specific topic
     */
    public long getQueueSize() {
        // This is a lightweight queue without persistent state tracking.
        // But we can not accurately get the queue size. So we return the difference between published and processed cont.
        return Math.max(0, publishedCount.get() - processedCount.get());
    }

    /**
     * shutdown the queue and release resources
     */
    public void shutdown() {
        log.info("Shutting down the MessageQueue...");
        
        timeWheel.stop();
        executor.shutdown();
        isRunning.set(false);
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                   log.error("Executor did not terminated properly, forcing shutdown.");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        queue.close();
        
        PerformanceStats finalStats = getPerformanceStats();
        log.info("Final performance stats: {}", finalStats);
        log.info("The message queue has been shut down.");
    }

    @Getter
	public static class PerformanceStats {
        private final long published;
        private final long processed;
        private final long failed;
        private final int topics;
        private final int consumers;

        public PerformanceStats(long published, long processed, long failed, int topics, int consumers) {
            this.published = published;
            this.processed = processed;
            this.failed = failed;
            this.topics = topics;
            this.consumers = consumers;
        }

		public double getProcessRate() {
            return published > 0 ? (double) processed / published : 0.0;
        }
        
        public double getFailureRate() {
            return processed > 0 ? (double) failed / processed : 0.0;
        }

        @Override
        public String toString() {
            return String.format("Stats{published:%d, processed:%d, failure:%d, success_rate:%.1f%%, topic:%d, consumer:%d}",
                published, processed, failed, getProcessRate() * 100, topics, consumers);
        }
    }
}