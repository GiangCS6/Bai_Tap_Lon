package bai_tap_lon.server.http;

import bai_tap_lon.server.config.ImageConfig;
import bai_tap_lon.server.network.ClientHandler;
import bai_tap_lon.server.service.ImageStorageService;
import bai_tap_lon.server.service.LocalImageStorageService;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class StaticFileServer {
    private static final Logger logger = Logger.getLogger(StaticFileServer.class.getName());

    public void start() throws IOException {
        //Server
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(ImageConfig.getPort()),0);

        // Parser và kho lưu trữ
        ImageStorageService storage = new LocalImageStorageService();
        MultipartParser multipartParser = new MultipartParser();

        //Context: 2 endpoint / upload và /uploads
        httpServer.createContext("/upload", new UploadHttpHandler(multipartParser, storage));
        httpServer.createContext("/uploads/", new StaticFileHandler());

        // Bắt đầu server
        httpServer.setExecutor(Executors.newFixedThreadPool(4));
        httpServer.start();
        logger.info("[StaticFileServer] Running on port " + ImageConfig.getPort());
    }
}

