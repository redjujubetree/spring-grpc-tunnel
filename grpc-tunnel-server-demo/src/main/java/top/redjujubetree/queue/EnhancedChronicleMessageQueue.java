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
import java.util.stream.Collectors;

/**
 * 增强版消息队列 - 完善竞争模式和广播模式的实现
 */
public class EnhancedChronicleMessageQueue {

    private static Logger log = LoggerFactory.getLogger("EnhancedChronicleMessageQueue");
    private final ChronicleQueue queue;
    private final ExcerptAppender appender;
    private final Map<String, List<ConsumerInfo>> subscriptions = new ConcurrentHashMap<>();
    private final ExecutorService executor;
    private final TimeWheel timeWheel;

    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    private final AtomicLong publishedCount = new AtomicLong(0);
    private final AtomicLong processedCount = new AtomicLong(0);
    private final AtomicLong failedCount = new AtomicLong(0);

    @Getter
    private static class ConsumerInfo {
        private final String topic;
        private final String groupId;
        private final String instanceId; // 新增：消费者实例ID
        private final TaskHandler handler;
        private final boolean enableBroadcast;
        private final ExcerptTailer tailer;

        public ConsumerInfo(String topic, String groupId, String instanceId, TaskHandler handler, 
                           boolean enableBroadcast, ExcerptTailer tailer) {
            this.topic = topic;
            this.groupId = groupId;
            this.instanceId = instanceId;
            this.handler = handler;
            this.enableBroadcast = enableBroadcast;
            this.tailer = tailer;
        }
    }

    public EnhancedChronicleMessageQueue(String path, int threadCount, int ticksPerWheel, long tickDurationMillis) {
        this.queue = SingleChronicleQueueBuilder.binary(path).build();
        this.appender = queue.acquireAppender();

        this.executor = new ThreadPoolExecutor(
            threadCount, threadCount, 60L, TimeUnit.SECONDS, 
            new ArrayBlockingQueue<>(1000), 
            r -> {
                Thread t = new Thread(r, "Consumer-Worker");
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );

        this.timeWheel = new TimeWheel(ticksPerWheel, tickDurationMillis, this::republishMessage);
        timeWheel.start();

        log.info("EnhancedChronicleMessageQueue started with path: {}, threadCount: {}, " +
                "ticksPerWheel: {}, tickDurationMillis: {}", 
                path, threadCount, ticksPerWheel, tickDurationMillis);
    }

    /**
     * 订阅主题 - 增强版，支持真正的竞争模式和广播模式
     */
    public void subscribe(String topic, ConsumerGroup consumerGroup, TaskHandler handler) {
        String instanceId = UUID.randomUUID().toString().substring(0, 8);
        
        if (consumerGroup.isEnableBroadcast()) {
            // 广播模式：每个消费者组有独立的tailer
            subscribeBroadcastMode(topic, consumerGroup, handler, instanceId);
        } else {
            // 竞争模式：同一消费者组共享tailer
            subscribeCompetitiveMode(topic, consumerGroup, handler, instanceId);
        }
    }

    /**
     * 广播模式订阅：每个消费者组都有独立的读取进度
     */
    private void subscribeBroadcastMode(String topic, ConsumerGroup consumerGroup, 
                                      TaskHandler handler, String instanceId) {
        String tailerId = topic + "-" + consumerGroup.getGroupId();
        ExcerptTailer tailer = queue.createTailer(tailerId);
        
        ConsumerInfo consumerInfo = new ConsumerInfo(topic, consumerGroup.getGroupId(), 
                                                   instanceId, handler, true, tailer);
        
        subscriptions.computeIfAbsent(topic, k -> new ArrayList<>()).add(consumerInfo);
        
        // 启动独立的消费者线程
        startConsumerThread(consumerInfo);
        
        log.info("Broadcast consumer registered: group={}, instance={}, topic={}", 
                consumerGroup.getGroupId(), instanceId, topic);
    }

    /**
     * 竞争模式订阅：同一消费者组的多个实例共享读取进度
     */
    private void subscribeCompetitiveMode(String topic, ConsumerGroup consumerGroup, 
                                        TaskHandler handler, String instanceId) {
        String sharedTailerId = topic + "-" + consumerGroup.getGroupId();
        
        synchronized (subscriptions) {
            List<ConsumerInfo> topicSubscriptions = subscriptions.computeIfAbsent(topic, k -> new ArrayList<>());
            
            // 检查是否已有该消费者组的共享tailer
            ExcerptTailer sharedTailer = null;
            for (ConsumerInfo existing : topicSubscriptions) {
                if (existing.getGroupId().equals(consumerGroup.getGroupId()) && !existing.isEnableBroadcast()) {
                    sharedTailer = existing.getTailer();
                    break;
                }
            }
            
            // 如果没有共享tailer，创建一个
            if (sharedTailer == null) {
                sharedTailer = queue.createTailer(sharedTailerId);
                // 启动共享的消费者线程组
                startCompetitiveConsumerGroup(topic, consumerGroup.getGroupId(), sharedTailer);
            }
            
            ConsumerInfo consumerInfo = new ConsumerInfo(topic, consumerGroup.getGroupId(), 
                                                       instanceId, handler, false, sharedTailer);
            topicSubscriptions.add(consumerInfo);
            
            log.info("Competitive consumer registered: group={}, instance={}, topic={}", 
                    consumerGroup.getGroupId(), instanceId, topic);
        }
    }

    /**
     * 启动竞争模式的消费者组 - 多个handler竞争处理消息
     */
    private void startCompetitiveConsumerGroup(String topic, String groupId, ExcerptTailer sharedTailer) {
        Thread competitiveReaderThread = new Thread(() -> {
            log.info("Starting competitive reader for group: {} on topic: {}", groupId, topic);
            
            int emptyReads = 0;
            
            while (isRunning.get()) {
                try (DocumentContext dc = sharedTailer.readingDocument()) {
                    if (!dc.isPresent()) {
                        emptyReads++;
                        long waitTime = Math.min(100, 10 + emptyReads * 5);
                        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(waitTime));
                        continue;
                    }
                    
                    emptyReads = 0;
                    QueueMessage msg = dc.wire().read("msg").object(QueueMessage.class);
                    
                    // 主题过滤
                    if (!topic.equals(msg.getTopic())) {
                        continue;
                    }
                    
                    // 消费者组过滤
                    if (Strings.isNotEmpty(msg.getConsumerGroup()) &&
                        !Objects.equals(msg.getConsumerGroup(), groupId)) {
                        continue;
                    }

                    long now = System.currentTimeMillis();
                    if (now >= msg.getNotBefore()) {
                        // 竞争模式：选择一个handler处理消息
                        processMessageCompetitively(msg, topic, groupId);
                    } else {
                        // 延时消息放入时间轮
                        long delay = msg.getNotBefore() - now;
                        timeWheel.add(msg, delay);
                    }
                    
                } catch (Exception e) {
                    log.error("Competitive reader error for group {}: ", groupId, e);
                    LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(100));
                }
            }
            
            log.info("Competitive reader stopped for group: {} on topic: {}", groupId, topic);
        }, "CompetitiveReader-" + topic + "-" + groupId);
        
        competitiveReaderThread.setDaemon(true);
        competitiveReaderThread.start();
    }

    /**
     * 启动独立的消费者线程（广播模式）
     */
    private void startConsumerThread(ConsumerInfo consumerInfo) {
        Thread consumerThread = new Thread(() -> {
            log.info("Starting broadcast consumer: group={}, instance={}, topic={}", 
                    consumerInfo.getGroupId(), consumerInfo.getInstanceId(), consumerInfo.getTopic());
            
            int emptyReads = 0;
            
            while (isRunning.get()) {
                try (DocumentContext dc = consumerInfo.getTailer().readingDocument()) {
                    if (!dc.isPresent()) {
                        emptyReads++;
                        long waitTime = Math.min(100, 10 + emptyReads * 5);
                        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(waitTime));
                        continue;
                    }
                    
                    emptyReads = 0;
                    QueueMessage msg = dc.wire().read("msg").object(QueueMessage.class);
                    
                    // 主题过滤
                    if (!consumerInfo.getTopic().equals(msg.getTopic())) {
                        continue;
                    }
                    
                    // 消费者组过滤
                    if (Strings.isNotEmpty(msg.getConsumerGroup()) &&
                        !Objects.equals(msg.getConsumerGroup(), consumerInfo.getGroupId())) {
                        continue;
                    }

                    long now = System.currentTimeMillis();
                    if (now >= msg.getNotBefore()) {
                        processMessage(msg, consumerInfo);
                    } else {
                        // 延时消息放入时间轮
                        long delay = msg.getNotBefore() - now;
                        timeWheel.add(msg, delay);
                    }
                    
                } catch (Exception e) {
                    log.error("Broadcast consumer error for {}: ", consumerInfo.getInstanceId(), e);
                    LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(100));
                }
            }
            
            log.info("Broadcast consumer stopped: group={}, instance={}, topic={}", 
                    consumerInfo.getGroupId(), consumerInfo.getInstanceId(), consumerInfo.getTopic());
        }, "BroadcastConsumer-" + consumerInfo.getTopic() + "-" + consumerInfo.getInstanceId());
        
        consumerThread.setDaemon(true);
        consumerThread.start();
    }

    /**
     * 竞争模式消息处理：从同一消费者组的多个handler中选择一个
     */
    private void processMessageCompetitively(QueueMessage msg, String topic, String groupId) {
        List<ConsumerInfo> topicSubscriptions = subscriptions.get(topic);
        if (topicSubscriptions == null) return;
        
        // 找到该消费者组的所有竞争实例
        List<ConsumerInfo> competitiveConsumers = topicSubscriptions.stream()
            .filter(consumer -> consumer.getGroupId().equals(groupId) && !consumer.isEnableBroadcast())
            .collect(Collectors.toList());
        
        if (competitiveConsumers.isEmpty()) return;
        
        // 使用消息ID的哈希值来选择处理器，保证同一消息总是被同一实例处理
        int selectedIndex = Math.abs(msg.getId().hashCode()) % competitiveConsumers.size();
        ConsumerInfo selectedConsumer = competitiveConsumers.get(selectedIndex);
        
        log.debug("Message {} assigned to competitive consumer instance: {}", 
                 msg.getId(), selectedConsumer.getInstanceId());
        
        processMessage(msg, selectedConsumer);
    }

    /**
     * 处理消息
     */
    private void processMessage(QueueMessage msg, ConsumerInfo consumerInfo) {
        executor.submit(() -> {
            try {
                consumerInfo.getHandler().handle(msg);
                processedCount.incrementAndGet();
                
                log.debug("Message {} processed by consumer: group={}, instance={}", 
                         msg.getId(), consumerInfo.getGroupId(), consumerInfo.getInstanceId());
                
            } catch (Exception e) {
                failedCount.incrementAndGet();
                handleRetry(msg, consumerInfo, e);
            }
        });
    }

    /**
     * 重新发布消息（时间轮回调）
     */
    private void republishMessage(QueueMessage msg) {
        msg.setNotBefore(System.currentTimeMillis());
        appender.writeDocument(w -> w.write("msg").object(msg));
        log.debug("Message {} republished after delay", msg.getId());
    }

    /**
     * 处理重试
     */
    private void handleRetry(QueueMessage msg, ConsumerInfo consumerInfo, Exception error) {
        if (msg.getRetryCount() < msg.getMaxRetries()) {
            msg.setRetryCount(msg.getRetryCount() + 1);
            long retryDelay = msg.getRetryDelayMillis() * msg.getRetryCount() * msg.getRetryCount();
            
            log.info("Retrying message [{}] for group [{}] instance [{}] - Attempt {}/{} after {}ms", 
                    msg.getId(), consumerInfo.getGroupId(), consumerInfo.getInstanceId(),
                    msg.getRetryCount(), msg.getMaxRetries(), retryDelay);
            
            msg.setNotBefore(System.currentTimeMillis() + retryDelay);
            appender.writeDocument(w -> w.write("msg").object(msg));
        } else {
            log.error("Message [{}] failed after {} retries for group [{}] instance [{}]: {}", 
                     msg.getId(), msg.getMaxRetries(), consumerInfo.getGroupId(), 
                     consumerInfo.getInstanceId(), error.getMessage());
            
            try {
                consumerInfo.getHandler().onError(msg, consumerInfo.getGroupId(), error);
            } catch (Exception handlerError) {
                log.error("Error handler failed for message [{}]", msg.getId(), handlerError);
            }
        }
    }

    /**
     * 发布消息
     */
    public void publish(QueueMessage msg) {
        appender.writeDocument(w -> w.write("msg").object(msg));
        publishedCount.incrementAndGet();
    }

    /**
     * 批量发布
     */
    public void publishBatch(List<QueueMessage> messages) {
        for (QueueMessage msg : messages) {
            publish(msg);
        }
    }

    /**
     * 获取性能统计
     */
    public PerformanceStats getPerformanceStats() {
        int totalConsumers = subscriptions.values().stream()
            .mapToInt(List::size)
            .sum();
            
        return new PerformanceStats(
            publishedCount.get(),
            processedCount.get(),
            failedCount.get(),
            subscriptions.size(),
            totalConsumers
        );
    }

    /**
     * 获取订阅统计
     */
    public Map<String, ConsumerGroupStats> getDetailedSubscriptionStats() {
        Map<String, ConsumerGroupStats> stats = new HashMap<>();
        
        subscriptions.forEach((topic, consumers) -> {
            Map<String, Integer> groupCounts = new HashMap<>();
            Map<String, String> groupModes = new HashMap<>();
            
            for (ConsumerInfo consumer : consumers) {
                String groupId = consumer.getGroupId();
                groupCounts.merge(groupId, 1, Integer::sum);
                groupModes.put(groupId, consumer.isEnableBroadcast() ? "BROADCAST" : "COMPETITIVE");
            }
            
            stats.put(topic, new ConsumerGroupStats(groupCounts, groupModes));
        });
        
        return stats;
    }

    /**
     * 消费者组统计信息
     */
    public static class ConsumerGroupStats {
        private final Map<String, Integer> groupCounts;
        private final Map<String, String> groupModes;
        
        public ConsumerGroupStats(Map<String, Integer> groupCounts, Map<String, String> groupModes) {
            this.groupCounts = groupCounts;
            this.groupModes = groupModes;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            groupCounts.forEach((groupId, count) -> {
                String mode = groupModes.get(groupId);
                sb.append(String.format("%s(%s):%d ", groupId, mode, count));
            });
            return sb.toString().trim();
        }
    }

    /**
     * 性能统计
     */
    @Getter
    public static class PerformanceStats {
        private final long published;
        private final long processed;
        private final long failed;
        private final int topics;
        private final int totalConsumers;

        public PerformanceStats(long published, long processed, long failed, int topics, int totalConsumers) {
            this.published = published;
            this.processed = processed;
            this.failed = failed;
            this.topics = topics;
            this.totalConsumers = totalConsumers;
        }

        public double getProcessRate() {
            return published > 0 ? (double) processed / published : 0.0;
        }
        
        public double getFailureRate() {
            return (processed + failed) > 0 ? (double) failed / (processed + failed) : 0.0;
        }

        @Override
        public String toString() {
            return String.format("Stats{published:%d, processed:%d, failed:%d, success:%.1f%%, topics:%d, consumers:%d}",
                published, processed, failed, getProcessRate() * 100, topics, totalConsumers);
        }
    }

    /**
     * 关闭队列
     */
    public void shutdown() {
        log.info("Shutting down EnhancedChronicleMessageQueue...");
        
        isRunning.set(false);
        timeWheel.stop();
        executor.shutdown();
        
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        queue.close();
        log.info("EnhancedChronicleMessageQueue shutdown completed");
    }
}