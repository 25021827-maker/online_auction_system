package dto;

import model.WalletTransaction;

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

    public static WalletTransactionDTO from(WalletTransaction tx) {
        WalletTransactionDTO dto = new WalletTransactionDTO();
        dto.transactionId = tx.getTransactionId();
        dto.userId = tx.getUserId();
        dto.auctionId = tx.getAuctionId();
        dto.type = tx.getType();
        dto.amount = tx.getAmount();
        dto.balanceBefore = tx.getBalanceBefore();
        dto.balanceAfter = tx.getBalanceAfter();
        dto.availableBefore = tx.getAvailableBefore();
        dto.availableAfter = tx.getAvailableAfter();
        dto.note = tx.getNote();
        dto.createdAt = tx.getCreatedAt() == null ? null : tx.getCreatedAt().toString();
        return dto;
    }
}
