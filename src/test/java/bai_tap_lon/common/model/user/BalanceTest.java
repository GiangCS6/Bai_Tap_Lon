package bai_tap_lon.common.model.user;

import bai_tap_lon.common.exception.NotEnoughBalanceException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BalanceTest {

    @Test
    void bidderDepositWithdrawAndRejectOverdraw() {
        Bidder bidder = new Bidder("bidder", "password", "bidder@example.com");

        bidder.deposit(1_000);
        bidder.withdraw(400);

        assertEquals(600, bidder.getBalance());
        assertThrows(IllegalArgumentException.class, () -> bidder.withdraw(601));
        assertEquals(600, bidder.getBalance());
    }

    @Test
    void sellerDepositWithdrawAndRejectOverdraw() {
        Seller seller = new Seller("seller", "password", "seller@example.com");

        seller.deposit(2_000);
        seller.withdraw(750);

        assertEquals(1_250, seller.getBalance());
        assertThrows(NotEnoughBalanceException.class, () -> seller.withdraw(1_251));
        assertEquals(1_250, seller.getBalance());
    }

    @Test
    void bidderAutoBidRejectsInvalidValuesAndBalanceOverLimit() {
        Bidder bidder = new Bidder("bidder", "password", "bidder@example.com");
        bidder.setBalance(5_000);

        assertThrows(IllegalArgumentException.class, () -> bidder.setAutoBid(5_001, 100));
        assertThrows(IllegalArgumentException.class, () -> bidder.setAutoBid(0, 100));
        assertThrows(IllegalArgumentException.class, () -> bidder.setAutoBid(1_000, 0));

        bidder.setAutoBid(4_000, 250);

        assertNotNull(bidder.getAutoBidSetting());
        assertTrue(bidder.getAutoBidSetting().isActive());
        assertEquals(4_000, bidder.getAutoBidSetting().getMaxBid());
        assertEquals(250, bidder.getAutoBidSetting().getIncrement());
    }

    @Test
    void cancelAutoBidMarksSettingInactive() {
        Bidder bidder = new Bidder("bidder", "password", "bidder@example.com");
        bidder.setBalance(5_000);
        bidder.setAutoBid(4_000, 250);

        bidder.cancelAutoBid();

        assertFalse(bidder.getAutoBidSetting().isActive());
    }
}
