package bai_tap_lon.server.http;

import bai_tap_lon.server.config.ImageConfig;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class StaticFileHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String requestPath = exchange.getRequestURI().getPath().replace("/uploads/", "");

        Path baseDir = Paths.get(ImageConfig.getUploadDir()).toAbsolutePath().normalize();
        Path targetPath = baseDir.resolve(requestPath).normalize();

        // chặn traversal
        if (!targetPath.startsWith(baseDir)) {
            exchange.sendResponseHeaders(403, 0);
            exchange.close();
            return;
        }

        File file = targetPath.toFile();

        if (!file.exists() || file.isDirectory()) {
            exchange.sendResponseHeaders(404, 0);
            exchange.close();
            return;
        }

        String mime = Files.probeContentType(file.toPath());
        if (mime == null) mime = "application/octet-stream";

        exchange.getResponseHeaders().set("Content-Type", mime);
        exchange.sendResponseHeaders(200, file.length());

        try (OutputStream out = exchange.getResponseBody();
             InputStream in = new FileInputStream(file)) {
            byte[] buf = new byte[8192];
            int read;
            while ((read = in.read(buf)) != -1) {
                out.write(buf, 0, read);
            }
        }
        exchange.close();
    }
}