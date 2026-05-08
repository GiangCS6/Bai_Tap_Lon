import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AuctionTest {

    @Test
    void testValidBid() {
        Auction auction = new Auction(100);
        boolean result = auction.placeBid(150);
        assertTrue(result);
        assertEquals(150, auction.getCurrentPrice());
    }

    @Test
    void testInvalidBid() {
        Auction auction = new Auction(100);
        boolean result = auction.placeBid(90);
        assertFalse(result);
        assertEquals(100, auction.getCurrentPrice());
    }

    @Test
    void testEndAuction() {
        Auction auction = new Auction(100);
        auction.endAuction();
        boolean result = auction.placeBid(200);
        assertFalse(result);
        assertTrue(auction.isEnded());
    }
}
