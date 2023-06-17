package bot.modules.subscription;

import java.time.LocalDateTime;
import java.util.Objects;

public class UserSubscriptionInfo {
    private int subscriptionId;
    private String userId;
    private LocalDateTime lastSubscription;

    public UserSubscriptionInfo(int subscriptionId, String userId) {
        this.subscriptionId = subscriptionId;
        this.userId = userId;
        lastSubscription = LocalDateTime.now();
    }

    public int getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(int subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public LocalDateTime getLastSubscription() {
        return lastSubscription;
    }

    public void setLastSubscription(LocalDateTime lastSubscription) {
        this.lastSubscription = lastSubscription;
    }

    public void writeNewSubscriptionTime() {
        lastSubscription = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserSubscriptionInfo that = (UserSubscriptionInfo) o;
        return subscriptionId == that.subscriptionId && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subscriptionId, userId);
    }
}
