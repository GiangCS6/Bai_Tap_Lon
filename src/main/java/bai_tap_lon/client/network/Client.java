package bai_tap_lon.client.network;

import bai_tap_lon.common.network.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client implements Runnable{

    /// Singleton vì các controller dùng chung connection
    private static Client instance;
    private static final Logger logger = Logger.getLogger(Client.class.getName());

    public static synchronized Client getInstance(){
        if(instance==null){
            instance = new Client();
        }
        return instance;
    }

    private Client(){}

    //  State
    // ────────────────────────────────────────
    private Socket socket;
    private BufferedWriter writer;
    private BufferedReader reader;

    private final Gson gson = new Gson();
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private Runnable onDisconnected;

    private final ExecutorService requestExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService readerExecutor = Executors.newSingleThreadExecutor();


    /// RUN bắt đầu vòng lặp đọc tin
    @Override
    public void run() {

        try {
            String line;
            while (connected.get() && (line = reader.readLine()) != null) {
                JsonObject object = gson.fromJson(line, JsonObject.class);
                ServerMessageRouter.route(object);
            }
        } catch (IOException e) {
            if (connected.get()) {
                logger.log(Level.SEVERE,"[Client] Mất kết nối đến server: ",e);
                connected.set(false);
                notifyDisconnected();
            }
        }
        logger.info("[Client] Reader thread kết thúc.");
    }

    /// NHƯ Serversocket.accept(), cần thông tin trước khi submit vào executor
    public void connect(String host, int port) throws IOException {
        if (connected.get()) {
            throw new IllegalStateException("You are already connected");
        }

        socket = new Socket(host, port);
        socket.setKeepAlive(true);
        socket.setSoTimeout(0); // không timeout khi đọc — server sẽ push bất cứ lúc nào

        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        connected.set(true);
        requestExecutor.submit(this);

        logger.info("[Client] Đã kết nối đến " + host + ":" + port);
    }

    /**
     * Ngắt kết nối và dọn dẹp tài nguyên.
     */
    public void disconnect() {
        if (!connected.get()) return;
        connected.set(false);

        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            logger.log(Level.SEVERE,"[Client] Lỗi khi ngắt kết nối: ",e);
        }

        readerExecutor.shutdownNow();
        requestExecutor.shutdownNow();

        logger.info("[Client] Đã ngắt kết nối.");
    }

    public void sendRequest(Request request){
        String json = gson.toJson(request);
        try{
            writer.write(json);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException("IO Error sending request to server");
        }
    }

    public void setOnDisconnected(Runnable callback){
        this.onDisconnected = callback;
    }

    public void notifyDisconnected(){
        Platform.runLater(onDisconnected);
    }



}





