public class Auction {
    private int currentPrice;
    private boolean ended;

    public Auction(int startingPrice) {
        this.currentPrice = startingPrice;
        this.ended = false;
    }

    public synchronized boolean placeBid(int bid) {
        if (ended) return false;
        if (bid > currentPrice) {
            currentPrice = bid;
            return true;
        }
        return false;
    }

    public void endAuction() {
        ended = true;
    }

    public int getCurrentPrice() {
        return currentPrice;
    }

    public boolean isEnded() {
        return ended;
    }
}

