package top.redjujubetree.queue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轻量级队列演示 - 接受重复投递的高性能模式
 */
public class LightweightDemo {
    
    public static void main(String[] args) throws InterruptedException {

        ChronicleMessageQueue queue =
            new ChronicleMessageQueue(
                "lightweight-queue", 8, 60, 1000
            );

        // 演示不同的消费模式
        demonstrateCompetitiveMode(queue);
        demonstrateBroadcastMode(queue);
        demonstrateHighThroughput(queue);
        
        // 等待处理
        System.out.println("⏱️ 等待消息处理...\n");
        Thread.sleep(10000);
        
        // 显示最终统计
        System.out.println("📊 最终性能统计:");
        System.out.println(queue.getPerformanceStats());
        System.out.println("📈 订阅统计: " + queue.getSubscriptionStats());
        System.out.println("📦 队列大小: " + queue.getQueueSize());
        
        queue.shutdown();
    }

    /**
     * 演示竞争模式 - 订单处理
     */
    private static void demonstrateCompetitiveMode(ChronicleMessageQueue queue) {
        System.out.println("🏁 演示场景1: 竞争模式 - 订单处理");
        System.out.println("特点: 每个订单通常只被一个服务处理（依赖Chronicle Queue的Tailer机制）\n");

        AtomicInteger orderService1Count = new AtomicInteger(0);
        AtomicInteger orderService2Count = new AtomicInteger(0);

        // 两个订单服务（竞争模式）
        ConsumerGroup orderService1 = new ConsumerGroup("order-service-1", "订单服务1", false);
        ConsumerGroup orderService2 = new ConsumerGroup("order-service-2", "订单服务2", false);

        queue.subscribe("order", orderService1, msg -> {
            OrderPayload payload = (OrderPayload) msg.getPayload();
            int count = orderService1Count.incrementAndGet();
            System.out.println("🛒 [订单服务1-" + count + "] 处理订单: " + payload.getOrderId() + 
                " 金额: ¥" + payload.getAmount());
        });

        queue.subscribe("order", orderService2, msg -> {
            OrderPayload payload = (OrderPayload) msg.getPayload();
            int count = orderService2Count.incrementAndGet();
            System.out.println("🛒 [订单服务2-" + count + "] 处理订单: " + payload.getOrderId() + 
                " 金额: ¥" + payload.getAmount());
        });

        // 发送订单消息
        for (int i = 1; i <= 5; i++) {
            QueueMessage orderMsg = QueueMessage.builder().id(UUID.randomUUID().toString())
                .payload(new OrderPayload("ORDER-" + i, "customer" + i, 100.0 * i))
                .topic("order")
                .notBefore(System.currentTimeMillis() + 1000)
                .retryCount(2)
                .retryDelayMillis(1000)
                .build();
            queue.publish(orderMsg);
        }
        
        System.out.println("✅ 已发送5个订单，观察分配情况\n");
    }

    /**
     * 演示广播模式 - 通知处理
     */
    private static void demonstrateBroadcastMode(ChronicleMessageQueue queue) {
        System.out.println("📢 演示场景2: 广播模式 - 通知处理");
        System.out.println("特点: 每个服务都会收到并处理通知（每个Tailer独立读取）\n");

        // 三个通知服务（广播模式）
        ConsumerGroup emailService = new ConsumerGroup("email-service", "邮件服务", true);
        ConsumerGroup smsService = new ConsumerGroup("sms-service", "短信服务", true);
        ConsumerGroup pushService = new ConsumerGroup("push-service", "推送服务", true);

        queue.subscribe("notification", emailService, msg -> {
            NotificationPayload payload = (NotificationPayload) msg.getPayload();
            System.out.println("📧 [邮件服务] 发送邮件给: " + payload.getUserId() + 
                " 内容: " + payload.getMessage());
        });

        queue.subscribe("notification", smsService, msg -> {
            NotificationPayload payload = (NotificationPayload) msg.getPayload();
            System.out.println("📱 [短信服务] 发送短信给: " + payload.getUserId() + 
                " 内容: " + payload.getMessage());
        });

        queue.subscribe("notification", pushService, msg -> {
            NotificationPayload payload = (NotificationPayload) msg.getPayload();
            System.out.println("🔔 [推送服务] 推送给: " + payload.getUserId() + 
                " 内容: " + payload.getMessage());
        });

        // 发送通知消息
        for (int i = 1; i <= 3; i++) {
            QueueMessage notificationMsg = QueueMessage.builder()
                    .id(UUID.randomUUID().toString())
                    .topic("notification")
                    .payload(new NotificationPayload("user" + i, "您有新消息 #" + i, "SYSTEM"))
                    .maxRetries(1)
                    .notBefore(System.currentTimeMillis() + 2000).build();
            queue.publish(notificationMsg);
        }
        
        System.out.println("✅ 已发送3个通知，每个服务都应该收到\n");
    }

    /**
     * 演示高吞吐量场景
     */
    private static void demonstrateHighThroughput(ChronicleMessageQueue queue) {
        System.out.println("⚡ 演示场景3: 高吞吐量处理");
        System.out.println("特点: 批量发送大量消息，测试性能\n");

        // 高性能日志处理服务
        ConsumerGroup logService = new ConsumerGroup("log-service", "日志服务", false);
        
        AtomicInteger logCount = new AtomicInteger(0);
        queue.subscribe("log", logService, msg -> {
            // 模拟快速日志处理
            int count = logCount.incrementAndGet();
            if (count % 50 == 0) {
                System.out.println("📝 [日志服务] 已处理 " + count + " 条日志");
            }
        });

        // 批量发送日志消息
        List<QueueMessage> logMessages = new ArrayList<>();
        for (int i = 1; i <= 200; i++) {
            QueueMessage logMsg = QueueMessage.builder()
                    .id(UUID.randomUUID().toString())
                    .topic("log")
                    .payload(new NotificationPayload("system", "日志条目 #" + i, "LOG"))
                    .notBefore(System.currentTimeMillis() + 3000)
                    .build();
            logMessages.add(logMsg);
        }
        
        // 使用批量发布提高性能
        queue.publishBatch(logMessages);
        System.out.println("✅ 已批量发送200条日志消息\n");
    }
}