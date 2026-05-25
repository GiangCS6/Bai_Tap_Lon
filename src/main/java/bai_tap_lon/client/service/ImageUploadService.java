package bai_tap_lon.client.service;

import bai_tap_lon.client.config.ClientImageConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class ImageUploadService {
    private static final String UPLOAD_ENDPOINT = ClientImageConfig.getUploadEndpoint();

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public CompletableFuture<String> uploadAsync(File file) {
        if (file == null) {
            return CompletableFuture.completedFuture("");
        }

        try {
            String boundary = "----Boundary" + System.currentTimeMillis();
            byte[] body = buildMultipartBody(file, boundary);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(UPLOAD_ENDPOINT))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(resp -> {
                        if (resp.statusCode() != 200) {
                            throw new RuntimeException("Upload failed with HTTP " + resp.statusCode());
                        }
                        JsonObject json = JsonParser.parseString(resp.body()).getAsJsonObject();
                        return json.get("imageUrl").getAsString();
                    });

        } catch (Exception e) {
            CompletableFuture<String> failed = new CompletableFuture<>();
            failed.completeExceptionally(e);
            return failed;
        }
    }

    private byte[] buildMultipartBody(File file, String boundary) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        String header = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\""
                + file.getName() + "\"\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n";
        out.write(header.getBytes(StandardCharsets.UTF_8));

        try (FileInputStream fileIn = new FileInputStream(file)) {
            byte[] buf = new byte[8192];
            int read;
            while ((read = fileIn.read(buf)) != -1) {
                out.write(buf, 0, read);
            }
        }

        String footer = "\r\n--" + boundary + "--\r\n";
        out.write(footer.getBytes(StandardCharsets.UTF_8));

        return out.toByteArray();
    }
}
