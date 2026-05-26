package service;

import com.google.gson.Gson;
import config.DatabaseConfig;
import dao.*;
import dto.AuctionDTO;
import dto.BidResult;
import entity.Auction;
import entity.Bid;
import entity.Item;
import exception.InvalidBidException;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class AuctionService {

    private final AuctionDAO auctionDAO = new AuctionDAO();
    private final AutoBidDAO autoBidDAO = new AutoBidDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();
    private final UserDAO userDAO = new UserDAO();
    private final ItemDAO itemDAO = new ItemDAO();
    private final AutoBidService autoBidService = new AutoBidService(this);
    private final Gson gson = new Gson();

    // ==================== Chuyển đổi Entity -> DTO (có itemName) ====================
    private AuctionDTO toDTO(Auction a) throws SQLException {
        AuctionDTO dto = new AuctionDTO();
        dto.setAuctionId(a.getAuctionId());
        dto.setItemId(a.getItemId());
        String itemName = itemDAO.getItemNameById(a.getItemId());
        dto.setItemName(itemName != null ? itemName : "Item #" + a.getItemId());
        dto.setCurrentPrice(a.getCurrentPrice());
        dto.setCurrentLeaderId(a.getCurrentLeaderId());
        dto.setStatus(a.getStatus());
        dto.setEndTime(a.getEndTime());
        dto.setMinBidStep(a.getMinBidStep());
        return dto;
    }

    // ==================== Lấy danh sách phiên đấu giá ====================
    public List<AuctionDTO> getActiveAuctionsDTO() throws SQLException {
        return auctionDAO.findActiveAuctions().stream()
                .map(auction -> {
                    try {
                        return toDTO(auction);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }

    public List<AuctionDTO> getAuctionsBySeller(Long sellerId) throws SQLException {
        return auctionDAO.findBySellerId(sellerId).stream()
                .map(auction -> {
                    try {
                        return toDTO(auction);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }

    public List<AuctionDTO> filterAuctions(String category, Double minPrice, Double maxPrice, String status) throws SQLException {
        return auctionDAO.filter(category, minPrice, maxPrice, status).stream()
                .map(auction -> {
                    try {
                        return toDTO(auction);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }

    // ==================== Tạo phiên đấu giá mới ====================
    public AuctionDTO createAuction(Long sellerId, String name, String description,
                                    BigDecimal startingPrice, LocalDateTime startTime,
                                    LocalDateTime endTime, BigDecimal minBidStep, String category) throws SQLException {
        // ----- VALIDATION (FIX LỖI 8) -----
        if (endTime.isBefore(startTime) || endTime.equals(startTime)) {
            throw new IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu");
        }
        if (startTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Không thể tạo phiên đấu giá trong quá khứ");
        }
        if (minBidStep.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Bước giá phải lớn hơn 0");
        }
        if (startingPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Giá khởi điểm không được âm");
        }
        // ---------------------------------

        Item item = new Item();
        item.setSellerId(sellerId);
        item.setName(name);
        item.setDescription(description);
        item.setStartingPrice(startingPrice);
        item.setCategory(category);
        item.setStatus("ACTIVE");
        Long itemId = itemDAO.createItem(item);

        Auction auction = new Auction();
        auction.setItemId(itemId);
        auction.setStartTime(startTime);
        auction.setEndTime(endTime);
        auction.setCurrentPrice(startingPrice);
        auction.setCurrentLeaderId(null);
        auction.setMinBidStep(minBidStep);
        auction.setStatus("OPEN");
        auction.setAntiSnipingSeconds(120);
        auction.setCreatedAt(LocalDateTime.now());
        Long auctionId = auctionDAO.createAuction(auction);

        return toDTO(auctionDAO.findById(auctionId));
    }

    // ==================== Đặt giá (phiên bản trả về JSON cho ClientHandler) ====================
    public String placeBid(Long userId, Long auctionId, BigDecimal amount) {
        BidResult result = placeBid(auctionId, userId, amount, false);
        return gson.toJson(result);
    }

    // ==================== Đặt giá (nội bộ, có transaction + retry khi deadlock + trả về số dư mới) ====================
    public BidResult placeBid(Long auctionId, Long bidderId, BigDecimal amount, boolean isAuto) {
        int maxRetries = 3;
        int attempt = 0;

        while (attempt < maxRetries) {
            Connection conn = null;
            try {
                conn = DatabaseConfig.getConnection();
                conn.setAutoCommit(false);

                Auction auction = auctionDAO.findByIdForUpdate(auctionId, conn);
                if (auction == null) {
                    throw new InvalidBidException("Auction không tồn tại");
                }
                if (!"OPEN".equals(auction.getStatus()) && !"RUNNING".equals(auction.getStatus())) {
                    throw new InvalidBidException("Phiên đấu giá không hoạt động");
                }
                if (auction.getEndTime().isBefore(LocalDateTime.now())) {
                    throw new InvalidBidException("Phiên đã kết thúc");
                }
                if (amount.compareTo(auction.getCurrentPrice().add(auction.getMinBidStep())) < 0) {
                    throw new InvalidBidException("Giá đặt phải >= " + auction.getCurrentPrice().add(auction.getMinBidStep()));
                }

                double balance = userDAO.getBalance(bidderId, conn);
                if (amount.doubleValue() > balance) {
                    return BidResult.failure("Số dư không đủ");
                }

                auctionDAO.insertBid(auctionId, bidderId, amount, isAuto, conn);

                Auction updated = auctionDAO.findByIdForUpdate(auctionId, conn);
                Bid highest = auctionDAO.getCurrentHighestBid(auctionId, conn);

                conn.commit();

                double newBalance = userDAO.getBalance(bidderId, conn);

                if (!isAuto) {
                    autoBidService.triggerAutoBid(auctionId, updated.getCurrentPrice());
                }

                return BidResult.success(updated.getCurrentPrice(), highest.getBidderId(),
                        updated.getEndTime(), BigDecimal.valueOf(newBalance));

            } catch (SQLException e) {
                if (conn != null) {
                    try {
                        conn.rollback();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }

                if (e.getSQLState() != null && (e.getSQLState().equals("40001") || e.getErrorCode() == 1205)) {
                    attempt++;
                    if (attempt >= maxRetries) {
                        return BidResult.failure("Hệ thống đang quá tải, vui lòng thử lại sau.");
                    }
                    try {
                        Thread.sleep(ThreadLocalRandom.current().nextInt(50, 200));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return BidResult.failure("Yêu cầu bị gián đoạn, vui lòng thử lại.");
                    }
                    continue;
                }

                if (e instanceof InvalidBidException) {
                    throw (InvalidBidException) e;
                }
                return BidResult.failure("Lỗi DB: " + e.getMessage());

            } catch (InvalidBidException e) {
                throw e;
            } finally {
                if (conn != null) {
                    try {
                        conn.setAutoCommit(true);
                        conn.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return BidResult.failure("Không thể xử lý yêu cầu sau nhiều lần thử.");
    }

    // ==================== Đóng các phiên hết hạn và trả về danh sách ID ====================
    public List<Long> closeExpiredAuctionsAndGetIds() throws SQLException {
        return auctionDAO.closeExpiredAuctionsAndReturnIds();
    }

    // ==================== Xử lý khi phiên kết thúc (gọi từ scheduler, có transaction) ====================
    public void processFinishedAuction(Long auctionId) throws SQLException {
        Connection conn = null;
        try {
            conn = DatabaseConfig.getConnection();
            conn.setAutoCommit(false);

            Auction auction = auctionDAO.findByIdForUpdate(auctionId, conn);
            if (auction == null || !"FINISHED".equals(auction.getStatus())) {
                conn.rollback();
                return;
            }

            Long winningBidId = auction.getWinningBidId();
            if (winningBidId != null) {
                Bid winningBid = auctionDAO.getBidById(winningBidId, conn);
                if (winningBid != null) {
                    double finalPrice = auction.getFinalPrice().doubleValue();

                    double winnerBalance = userDAO.getBalance(winningBid.getBidderId(), conn);
                    userDAO.updateBalance(winningBid.getBidderId(), winnerBalance - finalPrice, conn);

                    Long sellerId = itemDAO.getSellerIdByItemId(auction.getItemId(), conn);
                    double sellerBalance = userDAO.getBalance(sellerId, conn);
                    userDAO.updateBalance(sellerId, sellerBalance + finalPrice, conn);

                    notificationDAO.createNotification(winningBid.getBidderId(), auctionId,
                            "Chúc mừng! Bạn đã thắng đấu giá với giá " + finalPrice, conn);
                    notificationDAO.createNotification(sellerId, auctionId,
                            "Sản phẩm của bạn đã được bán với giá " + finalPrice, conn);

                    itemDAO.updateStatus(auction.getItemId(), "SOLD", conn);
                }
            }

            autoBidDAO.deleteByAuction(auctionId, conn);

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}