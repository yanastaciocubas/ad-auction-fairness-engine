package com.yonara.adauction.model;

public class AdBidEvent {

    private String bidId;
    private String advertiserId;
    private String adSlot;
    private double bidAmount;
    private long timestamp;
    private String userId;
    private UserProfile userProfile;

    public AdBidEvent() {}

    public AdBidEvent(String bidId, String advertiserId, String adSlot,
                      double bidAmount, long timestamp, String userId,
                      UserProfile userProfile) {
        this.bidId = bidId;
        this.advertiserId = advertiserId;
        this.adSlot = adSlot;
        this.bidAmount = bidAmount;
        this.timestamp = timestamp;
        this.userId = userId;
        this.userProfile = userProfile;
    }

    public String getBidId() { return bidId; }
    public String getAdvertiserId() { return advertiserId; }
    public String getAdSlot() { return adSlot; }
    public double getBidAmount() { return bidAmount; }
    public long getTimestamp() { return timestamp; }
    public String getUserId() { return userId; }
    public UserProfile getUserProfile() { return userProfile; }

    public void setBidId(String bidId) { this.bidId = bidId; }
    public void setAdvertiserId(String advertiserId) { this.advertiserId = advertiserId; }
    public void setAdSlot(String adSlot) { this.adSlot = adSlot; }
    public void setBidAmount(double bidAmount) { this.bidAmount = bidAmount; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setUserProfile(UserProfile userProfile) { this.userProfile = userProfile; }
}