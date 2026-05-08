package com.example.bai_tap_lon.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private static final int PORT = 5000;
    // Danh sách lưu trữ tất cả các luồng xử lý client đang kết nối
    private static List<ClientHandler> clients = new ArrayList<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("=== SERVER ĐẤU GIÁ ĐANG CHẠY TẠI PORT " + PORT + " ===");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("CÓ CLIENT MỚI KẾT NỐI: " + clientSocket.getInetAddress());

                // Khởi tạo một Handler riêng cho Client này
                ClientHandler clientHandler = new ClientHandler(clientSocket, clients);
                clients.add(clientHandler); // Thêm vào danh sách quản lý

                
                // Khởi chạy trên một Thread mới (Đa luồng)
                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Hàm Broadcast: Gửi một thông điệp (object) tới TẤT CẢ các Client đang kết nối.
     * Đây chính là cách triển khai tính năng Realtime Update (Observer Pattern qua mạng).
     */
    public static synchronized void broadcast(Object message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    // Hàm xóa client khi họ ngắt kết nối
    public static synchronized void removeClient(ClientHandler client) {
        clients.remove(client);
    }
}