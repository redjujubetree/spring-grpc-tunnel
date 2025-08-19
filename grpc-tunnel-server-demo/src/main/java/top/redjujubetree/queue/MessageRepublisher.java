package top.redjujubetree.queue;

@FunctionalInterface
public interface MessageRepublisher {
    void republish(QueueMessage msg);
}
