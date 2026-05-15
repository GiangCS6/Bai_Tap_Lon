package com.example.bai_tap_lon.client;

import java.io.*;
import java.net.*;
import javafx.application.Platform;

public class ClientHandler implements Runnable {

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private final AuctionListener listener;

    // Giao thức giống Server
    public interface AuctionListener {
        void onLoginSuccess(String username);
        void onLoginFailed(String message);
        void onItemListReceived(String rawList);           // "ITEM_LIST:name|price|bidder;..."
        void onNewBid(String itemName, double price, String bidder);
        void onSystemMessage(String message);
        void onError(String message);
        void onConnectionClosed();
    }

    // Constructor
    public ClientHandler(String host, int port, AuctionListener listener) throws IOException {
        this.listener = listener;
        this.socket = new Socket(host, port);
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new PrintWriter(socket.getOutputStream(), true);
        System.out.println("✅ Client đã kết nối Server: " + host + ":" + port);
    }

    // thử chạy processMessage nếu có line
    @Override
    public void run() {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                processMessage(line);
            }
        } catch (IOException e) {
            Platform.runLater(() -> listener.onConnectionClosed());
        } finally {
            closeConnection();
        }
    }

    //
    private void processMessage(String msg) {
        Platform.runLater(() -> {
            if (msg.startsWith("LOGIN_SUCCESS:")) {
                String user = msg.substring(14);
                listener.onLoginSuccess(user);

            } else if (msg.startsWith("LOGIN_FAILED:")) {
                String error = msg.substring(13);
                listener.onLoginFailed(error);

            } else if (msg.startsWith("ITEM_LIST:")) {
                String list = msg.substring(10);
                listener.onItemListReceived(list);

            } else if (msg.startsWith("NEW_BID:")) {
                String[] parts = msg.substring(8).split(":");
                if (parts.length == 3) {
                    String item = parts[0];
                    double price = Double.parseDouble(parts[1]);
                    String bidder = parts[2];
                    listener.onNewBid(item, price, bidder);
                }

            } else if (msg.startsWith("SYSTEM:")) {
                listener.onSystemMessage(msg.substring(7));

            } else if (msg.startsWith("ERROR:") || msg.startsWith("BID_FAILED:")) {
                listener.onError(msg.substring(msg.indexOf(":") + 1));
            }
        });
    }

    // ====================== CÁC HÀM GỬI LỆNH ======================
    public void login(String username, String password) {
        if (writer != null) writer.println("LOGIN:" + username + ":" + password);
    }

    public void bid(String itemName, double amount) {
        if (writer != null) writer.println("BID:" + itemName + ":" + amount);
    }

    public void requestItemList() {
        if (writer != null) writer.println("LIST");
    }

    public void logout() {
        if (writer != null) writer.println("LOGOUT");
        closeConnection();
    }

    private void closeConnection() {
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }
}