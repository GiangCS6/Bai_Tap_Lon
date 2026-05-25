package bai_tap_lon.common.model.user;
import bai_tap_lon.common.model.entity.Auction;
import bai_tap_lon.common.model.entity.AutoBidSetting;
import bai_tap_lon.common.model.entity.BidTransaction;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


public class Bidder extends User implements HasBalance {

    private List<BidTransaction> bidHistory;
    private AutoBidSetting autoBidSetting;
    private long balance;

    private static final Logger logger = Logger.getLogger(Bidder.class.getName());

    
    public Bidder(String username, String password, String email) {
        super(username, password, email);
        this.bidHistory = new ArrayList<>();
        this.balance = 0;
    }

    public Bidder(String username){
        super(username);
    }

    public BidTransaction placeBid(Auction auction, long amount) {
        if (amount > balance){
            throw new IllegalArgumentException("Không đủ số dư trong tài khoản!");
        }
        return auction.placeBid(this, amount);
    }

    public void setAutoBid(long maxBid, long increment) {
        if (maxBid > balance) {
            throw new IllegalArgumentException("MaxBid cant be greater than balance");
        }
        if(maxBid <= 0 || increment<=0){
            throw new IllegalArgumentException("MaxBid and Increment must be greater than 0");
        }
        this.autoBidSetting = new AutoBidSetting(maxBid, increment);
    }

    public void cancelAutoBid() {
        if (autoBidSetting != null) autoBidSetting.setActive(false);
    }

    public List<BidTransaction> getBidHistory() { return bidHistory; }
    public AutoBidSetting getAutoBidSetting() { return autoBidSetting; }

    @Override
    public synchronized void deposit(long amount){this.balance+=amount;}
    public synchronized void withdraw(long amount){
        if (amount>this.balance)
        {
            throw new IllegalArgumentException("Not enough balance");
        }
        else {
            this.balance-=amount;
        }
    }

    @Override
    public synchronized long getBalance(){
        return balance;
    }
    public synchronized void setBalance(long balance) { this.balance = balance; }


    @Override
    public String getRole(){
        return "BIDDER";
    }
    @Override
    public void printInfo() {
        logger.info("Bidder: " + getUsername());
        logger.info("Email: " + getEmail());
        logger.info("Bid History: " + bidHistory.size());
    }
}