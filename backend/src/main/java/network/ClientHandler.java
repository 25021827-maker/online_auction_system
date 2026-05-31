package network;

import com.google.gson.Gson;
import dto.RequestPayload;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable, ClientObserver {
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private Gson gson;
    private AuctionSubject auctionSubject;

    public ClientHandler(Socket socket, AuctionSubject subject) {
        this.clientSocket = socket;
        this.auctionSubject = subject;
        this.gson = new Gson();
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));


            auctionSubject.addObserver(this);

            String inputLine;

            while ((inputLine = in.readLine()) != null) {

                RequestPayload request = gson.fromJson(inputLine, RequestPayload.class);


                handleRequest(request);
            }
        } catch (Exception e) {
            System.out.println("Client ngắt kết nối.");
        } finally {
            auctionSubject.removeObserver(this);
            try { clientSocket.close(); } catch (Exception e) {}
        }
    }

    private void handleRequest(RequestPayload request) {
        switch (request.getAction()) {
            case "PLACE_BID":

                String updateMsg = "{\"event\": \"NEW_BID\", \"amount\": 500}";
                auctionSubject.notifyAllClients(updateMsg);
                break;
            case "LOGIN":
                out.println("{\"status\": \"SUCCESS\"}");
                break;
            default:
                out.println("{\"status\": \"ERROR\", \"message\": \"Unknown Action\"}");
        }
    }


    @Override
    public void sendRealtimeUpdate(String jsonData) {
        if (out != null) {
            out.println(jsonData);
        }
    }
}