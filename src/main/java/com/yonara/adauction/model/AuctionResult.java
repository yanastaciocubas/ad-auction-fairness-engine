package com.yonara.adauction.model;

public class AuctionResult {

    private String adSlot;
    private String winnerAdvertiserId;
    private double winningBid;
    private long auctionTime;
    private UserProfile winningUserProfile;

    public AuctionResult() {}

    public AuctionResult(String adSlot, String winnerAdvertiserId,
                         double winningBid, long auctionTime,
                         UserProfile winningUserProfile) {
        this.adSlot = adSlot;
        this.winnerAdvertiserId = winnerAdvertiserId;
        this.winningBid = winningBid;
        this.auctionTime = auctionTime;
        this.winningUserProfile = winningUserProfile;
    }

    public String getAdSlot() { return adSlot; }
    public String getWinnerAdvertiserId() { return winnerAdvertiserId; }
    public double getWinningBid() { return winningBid; }
    public long getAuctionTime() { return auctionTime; }
    public UserProfile getWinningUserProfile() { return winningUserProfile; }

    public void setAdSlot(String adSlot) { this.adSlot = adSlot; }
    public void setWinnerAdvertiserId(String id) { this.winnerAdvertiserId = id; }
    public void setWinningBid(double winningBid) { this.winningBid = winningBid; }
    public void setAuctionTime(long auctionTime) { this.auctionTime = auctionTime; }
    public void setWinningUserProfile(UserProfile p) { this.winningUserProfile = p; }
}