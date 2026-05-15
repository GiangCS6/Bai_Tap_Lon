package com.example.bai_tap_lon.network;

import com.example.bai_tap_lon.model.User; // Import lớp User của bạn

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private List<ClientHandler> clients;
    private String username = "Unknown"; // Tên của Client này

    public ClientHandler(Socket socket, List<ClientHandler> clients) {
        this.socket = socket;
        this.clients = clients;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            Object request;
            // Vòng lặp liên tục đọc yêu cầu từ Client
            while ((request = in.readObject()) != null) {

                // 1. Nếu Client gửi lên 1 chuỗi String (Ví dụ: "BID_1000")
                if (request instanceof String) {
                    String reqStr = (String) request;

                    if (reqStr.startsWith("BID_")) {
                        // Tách lấy số tiền đặt giá
                        double bidAmount = Double.parseDouble(reqStr.split("_")[1]);

                        // Xử lý luồng đấu giá an toàn
                        AuctionManager manager = AuctionManager.getInstance();
                        boolean success = manager.placeBid(this.username, bidAmount);

                        if (success) {
                            // Nếu thành công, PHÁT THANH (Broadcast) cho TẤT CẢ mọi người biết giá mới
                            String noti = "NEW_HIGH_BID_" + manager.getCurrentWinner() + "_" + manager.getCurrentHighestBid();
                            Server.broadcast(noti);
                        } else {
                            // Chỉ báo lỗi riêng cho client vừa đặt sai
                            sendMessage("BID_FAILED_Giá phải cao hơn " + manager.getCurrentHighestBid());
                        }
                    }
                }

                // 2. Nếu Client gửi lên 1 Object User (Ví dụ để Login)
                else if (request instanceof User) {
                    User user = (User) request;
                    this.username = user.getUsername();
                    System.out.println(this.username + " đã đăng nhập vào phiên đấu giá.");
                    sendMessage("LOGIN_SUCCESS");
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Client " + username + " ngắt kết nối.");
        } finally {
            Server.removeClient(this);
            closeAll();
        }
    }

    // Hàm dùng để gửi dữ liệu từ Server VỀ Client này
    public void sendMessage(Object message) {
        try {
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeAll() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}