package top.redjujubetree.queue;

import java.util.Objects;

/**
 * ConsumerGroup
 */
public class ConsumerGroup {
    private final String groupId;
    private final String description;
    private final boolean enableBroadcast;

    public ConsumerGroup(String groupId, String description, boolean enableBroadcast) {
        this.groupId = groupId;
        this.description = description;
        this.enableBroadcast = enableBroadcast;
    }

    public String getGroupId() { 
        return groupId; 
    }
    
    public String getDescription() { 
        return description; 
    }
    
    public boolean isEnableBroadcast() { 
        return enableBroadcast; 
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConsumerGroup that = (ConsumerGroup) o;
        return Objects.equals(groupId, that.groupId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId);
    }

    @Override
    public String toString() {
        return "ConsumerGroup{" +
                "groupId='" + groupId + '\'' +
                ", description='" + description + '\'' +
                ", enableBroadcast=" + enableBroadcast +
                '}';
    }
}