import java.io.*;
import java.net.*;
import java.util.Scanner;

public class SimpleClient {
    public static void main(String[] args) {
        try {
            // Kết nối tới server ở localhost và cổng 1234
            Socket socket = new Socket("localhost", 1234);
            System.out.println("Đã kết nối tới server!");

            // Tạo luồng đọc và ghi dữ liệu
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            // Nhập và gửi dữ liệu tới server
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("Nhập giá đấu (hoặc 'exit' để thoát): ");
                String input = scanner.nextLine();
                if (input.equalsIgnoreCase("exit")) break;

                out.println(input); // gửi giá đấu tới server
                String response = in.readLine(); // nhận phản hồi
                System.out.println("Server trả lời: " + response);
            }

            // Nhận phản hồi từ server
            String response = in.readLine();
            System.out.println("Server trả lời: " + response);

            // Đóng kết nối
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
