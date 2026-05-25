package bai_tap_lon.server.network;

import bai_tap_lon.common.model.entity.AuctionManager;
import bai_tap_lon.server.dao.*;
import bai_tap_lon.server.http.StaticFileServer;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerStart implements AutoCloseable {
    private static final int defaultPort = 8000;
    private final int port;
    private final Gson gson = new Gson();
    private final AuctionManager auctionManager;
    private final RequestRouter requestRouter;
    private  ServerSocket serverSocket;
    private final ExecutorService pool;
    // Giữ list ClientHandler chỉ để shutdown — broadcast đã do AuctionManager
    // + ClientHandler tự lo qua observer pattern.
    private final CopyOnWriteArrayList<ClientHandler> connectedClient = new CopyOnWriteArrayList<>();

    private volatile boolean running = false;

    //Constructor
    public ServerStart(int port){
        this.port = port;
        this.auctionManager = AuctionManager.getInstance();

        UserDao userdao=new UserDao();
        AuctionDao auctiondao=new AuctionDao();
        BidDao bidDAO=new BidDao();
        WatchListDao watchListDAO=new WatchListDao();
        ItemDAO itemDAO=new ItemDAO();
        DatabaseInitializer.initialize();
        this.auctionManager.init(userdao,auctiondao);

        this.requestRouter = new RequestRouter(userdao, auctiondao,bidDAO,itemDAO,auctionManager,watchListDAO);
        this.auctionManager.recoverFromDb(this.requestRouter.getBidExecutorFactory());
        int threads = Runtime.getRuntime().availableProcessors() * 4;
        this.pool = Executors.newFixedThreadPool(threads);
    }

    public ServerStart(){
        this(defaultPort);
    }

    ///start the server
    public void start() throws IOException {
        if (running) throw new IllegalStateException("Server is already running");
        serverSocket = new ServerSocket(port);
        running = true;

        new StaticFileServer().start();
        while(running){
            try{
                Socket client = serverSocket.accept(); // listen for connections, blocking call
                client.setKeepAlive(true);

                ClientHandler handler = new ClientHandler(client, this, getRequestRouter());
                connectedClient.add(handler);
                pool.submit(handler);//co the loi


            }
            catch(SocketException se){
                if (running){
                    System.err.println("Socket exception: " + se.getMessage());
                }
                break;
            }
            catch (IOException e){
                if (running){
                    System.err.println("IO exception: " + e.getMessage());
                }
            }
        }
    }

    ///-------Stop Server----------
    public void stop() throws IOException{
        if (!running) return;
        // running = false first so any exception in start can check first
        running = false;

        //close serverSocket
        if (serverSocket != null && !serverSocket.isClosed() ){
            serverSocket.close();
        }

        //clear client
        for(ClientHandler handler: connectedClient){
            handler.disconnect();
        }
        connectedClient.clear();

        //shutdown pool
        pool.shutdown();
    }

    @Override
    public void close() throws Exception{
        stop();
    }


    ///GETTER
    public Gson getGson() {
        return gson;
    }
    public boolean isRunning(){
        return running;
    }
    public AuctionManager getAuctionManager() {
        return auctionManager;
    }
    public RequestRouter getRequestRouter() {
        return requestRouter;
    }

    public void removeClient(ClientHandler clientHandler){
        connectedClient.remove(clientHandler);
    }

    /// Main create Server
    public static void main(String args[]){
        int port = defaultPort;
        if (args.length > 0){
            try {
                port = Integer.parseInt(args[0]);
            }
            catch(NumberFormatException nfe){
                System.err.println("Invalid port number, using default: " + defaultPort);
            }
        }

        ServerStart server = new ServerStart(port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.stop();
            } catch (IOException e){
                System.exit(1);
            }
        }));

        try {
            server.start();
        } catch(IOException e){
            System.err.println("[Server] Failed to start: " + e.getMessage());
        }

    }




}