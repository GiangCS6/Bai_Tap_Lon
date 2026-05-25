package bai_tap_lon.common.model.user;

import bai_tap_lon.common.model.entity.Auction;
import bai_tap_lon.common.model.entity.AuctionManager;


import java.util.logging.Logger;

public class Admin extends User {

    private static final Logger logger = Logger.getLogger(Admin.class.getName());

    public Admin(String username, String password, String email) {
        super(username, password, email);
    }

    public void banUser(User user) {
        user.setActive(false);
        logger.info(user.getUsername()+" has been Banned");
    }

    public void unbanUser(User user) {
        user.setActive(true);
        logger.info(user.getUsername()+" has been Unbanned");
    }

    public void cancelAuction(Auction auction,String reason) {
        AuctionManager.getInstance().cancelAuction(auction.getId(),reason);
        logger.info("Auction " + auction.getId()+" has been cancelled");
    }


    @Override
    public String getRole(){
        return "ADMIN";
    }
    @Override
    public void printInfo() {
        logger.info("Admin: " + getUsername());
        logger.info("Email: " + getEmail());
    }
}