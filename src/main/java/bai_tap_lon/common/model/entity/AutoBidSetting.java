package bai_tap_lon.common.model.entity;
public class AutoBidSetting {

    private long maxBid;
    private long increment;
    private boolean isActive;

    public AutoBidSetting(long maxBid, long increment) {
        if(maxBid <=0 ){throw new IllegalArgumentException("MaxBid must be positive");}
        if(increment<=0){throw new IllegalArgumentException("Increment must be positive");}
        this.maxBid = maxBid;
        this.increment = increment;
        this.isActive = true;
    }
    public long getMaxBid() { return maxBid; }
    public long getIncrement() { return increment; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }
    public void setMaxBid(long maxBid) { this.maxBid = maxBid; }
    public void setIncrement(long increment) { this.increment = increment; }

}