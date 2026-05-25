package bai_tap_lon.common.model.user;

public interface HasBalance {
    long getBalance();
    void setBalance(long balance);
    void deposit(long amount);
    void withdraw(long amount);
}
