package Service.auction;

import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;

public class AuctionRoomManager {

    private static final Map<Integer, Integer> viewers =
            new HashMap<>();

    public static void joinRoom(int productId) {

        viewers.put(

                productId,

                viewers.getOrDefault(productId, 0) + 1
        );
    }

    public static void joinRoom(
            int productId,
            Stage stage
    ) {

        joinRoom(productId);

        stage.setOnCloseRequest(e -> {

            leaveRoom(productId);
        });
    }


    public static void leaveRoom(int productId) {

        int current =
                viewers.getOrDefault(productId, 0);

        if (current <= 1) {

            viewers.remove(productId);

        } else {

            viewers.put(productId, current - 1);
        }
    }

    public static int getViewerCount(
            int productId
    ) {

        return viewers.getOrDefault(productId, 0);
    }
}
