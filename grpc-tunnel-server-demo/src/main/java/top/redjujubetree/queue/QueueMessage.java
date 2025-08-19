package top.redjujubetree.queue;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class QueueMessage implements Serializable {
    private String id;
    private String topic;
    private String consumerGroup; // consumer group identifier
    private Object payload;
    private long notBefore;         // delay until this timestamp to be consumable
    @Builder.Default
    private int retryCount = 0;
    @Builder.Default
    private int maxRetries = 3; // maximum retry attempts
    private long retryDelayMillis;

}
