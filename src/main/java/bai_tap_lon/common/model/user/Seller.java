package bai_tap_lon.common.model.user;
import bai_tap_lon.common.exception.NotEnoughBalanceException;
import bai_tap_lon.common.model.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class Seller extends User implements HasBalance {

    private final List<Item> itemsPosted;
    private long balance;

    private static final Logger logger = Logger.getLogger(Seller.class.getName());


    public Seller(String username){
        super(username);
        this.itemsPosted = new ArrayList<>();
    }

    public Seller(String username, String password, String email) {
        super(username, password, email);
        this.itemsPosted = new ArrayList<>();
        this.balance = 0;
    }

    public void postItem(Item item) {
        itemsPosted.add(item);
    }

    public void removeItem(String itemId) {
        itemsPosted.removeIf(item -> item.getId().equals(itemId));
    }

    public List<Item> getItemsPosted() { return itemsPosted; }

    @Override
    public String getRole(){
        return "SELLER";
    }
    @Override
    public void deposit(long amount){this.balance+=amount;}
    public void withdraw(long amount){
        if (amount>this.balance)
        {
            throw new NotEnoughBalanceException("Insufficient balance in account");
        }
        else {
            this.balance-=amount;
        }
    }
    public long getBalance(){
        return balance;
    }
    public void setBalance(long balance) { this.balance = balance; }


    @Override
    public void printInfo() {
        logger.info("Seller: " + getUsername());
        logger.info("Email: " + getEmail());
        logger.info("Items have been Posted: " + itemsPosted.size());
    }
}