package dto;

public class WalletTransactionDTO {
    public Long transactionId;
    public Long userId;
    public Long auctionId;
    public String type;
    public double amount;
    public double balanceBefore;
    public double balanceAfter;
    public double availableBefore;
    public double availableAfter;
    public String note;
    public String createdAt;
}
