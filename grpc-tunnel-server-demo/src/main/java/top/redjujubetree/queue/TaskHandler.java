package top.redjujubetree.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@FunctionalInterface
public interface TaskHandler {
    Logger log = LoggerFactory.getLogger(TaskHandler.class);
    void handle(QueueMessage msg) throws Exception;

    default void onError(QueueMessage msg, String consumerGroup, Exception error) {
        log.error("Message [{}] failed after {} retries for group [{}]: {}",
                msg.getId(), msg.getMaxRetries(), consumerGroup, error);
    }
}
