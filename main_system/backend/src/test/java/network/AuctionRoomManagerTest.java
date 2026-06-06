package network;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuctionRoomManagerTest {

    private ClientHandler mockClient1;
    private ClientHandler mockClient2;

    @BeforeEach
    public void setUp() {
        // Tạo các ClientHandler giả lập
        mockClient1 = mock(ClientHandler.class);
        mockClient2 = mock(ClientHandler.class);

        // Đảm bảo trạng thái sạch trước mỗi lần test
        AuctionRoomManager.removeClientFromAllRooms(mockClient1);
        AuctionRoomManager.removeClientFromAllRooms(mockClient2);
    }

    @Test
    public void testJoinAndBroadcastToRoom() {
        Long auctionId = 100L;

        // Cho 2 client join vào cùng một phòng
        AuctionRoomManager.joinRoom(auctionId, mockClient1);
        AuctionRoomManager.joinRoom(auctionId, mockClient2);

        // Phát tin nhắn vào phòng
        String testMessage = "{\"action\":\"BID_UPDATE\", \"data\":\"500\"}";
        AuctionRoomManager.broadcastToRoom(auctionId, testMessage);

        // Kiểm tra: Cả 2 client đều phải nhận được thông báo
        verify(mockClient1, times(1)).sendMessage(testMessage);
        verify(mockClient2, times(1)).sendMessage(testMessage);
    }

    @Test
    public void testLeaveRoom() {
        Long auctionId = 200L;

        AuctionRoomManager.joinRoom(auctionId, mockClient1);
        AuctionRoomManager.joinRoom(auctionId, mockClient2);

        // Client 1 rời phòng
        AuctionRoomManager.leaveRoom(auctionId, mockClient1);

        // Phát tin nhắn vào phòng
        AuctionRoomManager.broadcastToRoom(auctionId, "Message after leave");

        // Client 2 nhận được, Client 1 thì không
        verify(mockClient2, times(1)).sendMessage("Message after leave");
        verify(mockClient1, never()).sendMessage(anyString());
    }

    @Test
    public void testRemoveClientFromAllRooms_OnDisconnect() {
        // Client join nhiều phòng khác nhau
        AuctionRoomManager.joinRoom(10L, mockClient1);
        AuctionRoomManager.joinRoom(20L, mockClient1);

        // Giả lập client bị ngắt kết nối (hàm này sẽ xóa khỏi mọi phòng)
        AuctionRoomManager.removeClientFromAllRooms(mockClient1);

        // Broadcast vào phòng 10L và 20L
        AuctionRoomManager.broadcastToRoom(10L, "MSG A");
        AuctionRoomManager.broadcastToRoom(20L, "MSG B");

        // Kiểm tra: Client 1 không nhận được gì cả
        verify(mockClient1, never()).sendMessage(anyString());
    }

    @Test
    public void testBroadcastToNonExistentRoom() {
        // Broadcast vào một auctionId không tồn tại
        assertDoesNotThrow(() -> {
            AuctionRoomManager.broadcastToRoom(999L, "Should not crash");
        });
    }
}