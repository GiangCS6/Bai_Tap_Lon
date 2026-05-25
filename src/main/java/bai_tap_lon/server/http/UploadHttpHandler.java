package bai_tap_lon.server.http;

import bai_tap_lon.server.service.ImageStorageService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class UploadHttpHandler implements HttpHandler {

    private final MultipartParser multipartParser;
    private final ImageStorageService storage;

    public UploadHttpHandler(MultipartParser multipartParser, ImageStorageService storage){
        this.multipartParser = multipartParser;
        this.storage = storage;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        // check đúng là POST
        if (!"POST".equals(httpExchange.getRequestMethod())) {
            httpExchange.sendResponseHeaders(405, 0);
            httpExchange.close();
            return;
        }

        // lấy dữ liệu
        String contentType = httpExchange.getRequestHeaders().getFirst("Content-Type");
        byte[] body = httpExchange.getRequestBody().readAllBytes();

        // xử lí
        try{
            FilePart part = multipartParser.parse(contentType, body);
            String imageUrl = storage.save(part.getBytes(), part.getFilename());

            String resp = "{\"imageUrl\":\"" + imageUrl + "\"}";
            byte[] respBytes = resp.getBytes(StandardCharsets.UTF_8);

            httpExchange.getResponseHeaders().set("Content-Type", "application/json");
            httpExchange.sendResponseHeaders(200, respBytes.length);
            httpExchange.getResponseBody().write(respBytes);
        } catch (Exception e) {
            String resp = "{\"error\":\"" + e.getMessage() + "\"}";
            byte[] respBytes = resp.getBytes(StandardCharsets.UTF_8);
            httpExchange.sendResponseHeaders(400, respBytes.length);
            httpExchange.getResponseBody().write(respBytes);
        } finally {
            httpExchange.close();
        }
    }
}

