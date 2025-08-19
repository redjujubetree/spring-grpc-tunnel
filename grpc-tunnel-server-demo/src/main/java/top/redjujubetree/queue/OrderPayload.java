package top.redjujubetree.queue;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单消息载荷类
 * 用于封装订单类型的消息数据
 */
public class OrderPayload implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String orderId;
    private String customerId;
    private BigDecimal amount;
    private String status;
    private String currency;
    private long createdTime;

    public OrderPayload() {
        this.createdTime = System.currentTimeMillis();
        this.currency = "CNY";
        this.status = "PENDING";
    }

    public OrderPayload(String orderId, double amount) {
        this();
        this.orderId = orderId;
        this.amount = BigDecimal.valueOf(amount);
    }

    public OrderPayload(String orderId, String customerId, double amount) {
        this(orderId, amount);
        this.customerId = customerId;
    }

    public String getOrderId() { 
        return orderId; 
    }
    
    public void setOrderId(String orderId) { 
        this.orderId = orderId; 
    }
    
    public String getCustomerId() { 
        return customerId; 
    }
    
    public void setCustomerId(String customerId) { 
        this.customerId = customerId; 
    }
    
    public BigDecimal getAmount() { 
        return amount; 
    }
    
    public void setAmount(BigDecimal amount) { 
        this.amount = amount; 
    }
    
    public void setAmount(double amount) { 
        this.amount = BigDecimal.valueOf(amount); 
    }
    
    public String getStatus() { 
        return status; 
    }
    
    public void setStatus(String status) { 
        this.status = status; 
    }
    
    public String getCurrency() { 
        return currency; 
    }
    
    public void setCurrency(String currency) { 
        this.currency = currency; 
    }
    
    public long getCreatedTime() { 
        return createdTime; 
    }
    
    public void setCreatedTime(long createdTime) { 
        this.createdTime = createdTime; 
    }

    @Override
    public String toString() {
        return "OrderPayload{" +
                "orderId='" + orderId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", amount=" + amount +
                ", status='" + status + '\'' +
                ", currency='" + currency + '\'' +
                ", createdTime=" + createdTime +
                '}';
    }
}