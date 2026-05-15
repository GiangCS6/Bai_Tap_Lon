package com.example.bai_tap_lon.network;

import javafx.application.Platform;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Lớp quản lý kết nối mạng phía Client.
 * Sử dụng Singleton Pattern để dùng chung 1 kết nối cho toàn bộ ứng dụng JavaFX.
 */
public class NetworkClient {
    private static NetworkClient instance;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    // Bộ lắng nghe (Observer) để báo cho Controller biết khi có tin nhắn từ Server
    private MessageListener messageListener;

    private NetworkClient() {}

    public static synchronized NetworkClient getInstance() {
        if (instance == null) {
            instance = new NetworkClient();
        }
        return instance;
    }

    /**
     * Mở kết nối tới Server và bắt đầu luồng lắng nghe
     */
    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        // Lưu ý: Luôn khởi tạo OutputStream trước InputStream
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());

        System.out.println("Đã kết nối thành công tới Server " + host + ":" + port);

        // Khởi tạo một Luồng chạy ngầm (Background Thread) để liên tục nghe ngóng Server
        Thread listenerThread = new Thread(() -> {
            try {
                Object response;
                // Vòng lặp liên tục chờ tin nhắn (sẽ block ở đây cho đến khi có dữ liệu)
                while ((response = in.readObject()) != null) {
                    if (messageListener != null) {
                        final Object finalResponse = response;

                        // [KIẾN THỨC BẮT BUỘC JAVAFX]
                        // Khi luồng ngầm nhận được dữ liệu, muốn thay đổi UI (ví dụ đổi chữ Label)
                        // BẮT BUỘC phải đưa tác vụ đó vào Platform.runLater()
                        Platform.runLater(() -> messageListener.onMessageReceived(finalResponse));
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Đã ngắt kết nối khỏi máy chủ hoặc máy chủ đóng cửa.");
            }
        });

        // Đặt thành Daemon Thread để luồng tự chết khi người dùng bấm dấu X tắt ứng dụng
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    /**
     * Gửi yêu cầu (Object) lên Server
     */
    public void sendRequest(Object request) {
        try {
            if (out != null) {
                out.writeObject(request);
                out.flush(); // Bắt buộc phải flush để đẩy gói tin đi ngay lập tức
            }
        } catch (IOException e) {
            System.err.println("Lỗi khi gửi dữ liệu lên Server: " + e.getMessage());
        }
    }

    /**
     * Đăng ký Controller hiện tại để nhận tin nhắn
     */
    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }

    /**
     * Ngắt kết nối dọn dẹp tài nguyên
     */
    public void disconnect() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * [KIẾN THỨC: Observer Pattern cơ bản]
     * Interface này đóng vai trò như một "đường dây nóng".
     * Các Controller (như LoginController) sẽ implements nó để nhận thông báo.
     */
    public interface MessageListener {
        void onMessageReceived(Object message);
    }
}
