package top.redjujubetree.queue;

/**
 * 订阅信息类
 * 包含主题、消费者组和处理器的映射关系
 */
public class SubscriptionInfo {
    private final String topic;
    private final ConsumerGroup consumerGroup;
    private final TaskHandler handler;

    public SubscriptionInfo(String topic, ConsumerGroup consumerGroup, TaskHandler handler) {
        this.topic = topic;
        this.consumerGroup = consumerGroup;
        this.handler = handler;
    }

    public String getTopic() { 
        return topic; 
    }
    
    public ConsumerGroup getConsumerGroup() { 
        return consumerGroup; 
    }
    
    public TaskHandler getHandler() { 
        return handler; 
    }

    @Override
    public String toString() {
        return "SubscriptionInfo{" +
                "topic='" + topic + '\'' +
                ", consumerGroup=" + consumerGroup.getGroupId() +
                '}';
    }
}