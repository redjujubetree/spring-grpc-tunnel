package top.redjujubetree.queue;

import java.io.Serializable;

/**
 * 通知消息载荷类
 * 用于封装通知类型的消息数据
 */
public class NotificationPayload implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String userId;
    private String message;
    private String notificationType;
    private long timestamp;

    public NotificationPayload() {
        this.timestamp = System.currentTimeMillis();
    }

    public NotificationPayload(String userId, String message) {
        this();
        this.userId = userId;
        this.message = message;
        this.notificationType = "GENERAL";
    }

    public NotificationPayload(String userId, String message, String notificationType) {
        this();
        this.userId = userId;
        this.message = message;
        this.notificationType = notificationType;
    }

    public String getUserId() { 
        return userId; 
    }
    
    public void setUserId(String userId) { 
        this.userId = userId; 
    }
    
    public String getMessage() { 
        return message; 
    }
    
    public void setMessage(String message) { 
        this.message = message; 
    }
    
    public String getNotificationType() { 
        return notificationType; 
    }
    
    public void setNotificationType(String notificationType) { 
        this.notificationType = notificationType; 
    }
    
    public long getTimestamp() { 
        return timestamp; 
    }
    
    public void setTimestamp(long timestamp) { 
        this.timestamp = timestamp; 
    }

    @Override
    public String toString() {
        return "NotificationPayload{" +
                "userId='" + userId + '\'' +
                ", message='" + message + '\'' +
                ", notificationType='" + notificationType + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}