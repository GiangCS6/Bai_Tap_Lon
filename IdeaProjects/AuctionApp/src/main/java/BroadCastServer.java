import java.io.*;
import java.net.*;
import java.util.*;

public class BroadCastServer {
    // Danh sách tất cả client đang kết nối
    private static List<PrintWriter> clientOutputs = new ArrayList<>();

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(1234);
            System.out.println("Server đang chờ kết nối...");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client mới đã kết nối!");

                // Tạo thread xử lý client
                new ClientHandler(socket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Lớp xử lý từng client
    static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Thêm output của client vào danh sách chung
                synchronized (clientOutputs) {
                    clientOutputs.add(out);
                }

                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("Client gửi: " + message);
                    broadcast("Server broadcast: " + message);
                }

                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Hàm gửi thông điệp tới tất cả client
        private void broadcast(String message) {
            synchronized (clientOutputs) {
                for (PrintWriter writer : clientOutputs) {
                    writer.println(message);
                }
            }
        }
    }
}
