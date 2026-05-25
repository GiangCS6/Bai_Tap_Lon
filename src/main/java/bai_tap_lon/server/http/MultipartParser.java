package bai_tap_lon.server.http;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class MultipartParser {

    public FilePart parse(String contentType, byte[] body) {
        // Implement multipart/form-data parsing logic here
        if (contentType == null || !contentType.contains("boundary=")) {
            throw new IllegalArgumentException("Missing Boundary");
        }
        // lấy boundary
        String boundary = "--" + contentType.split("boundary=")[1].trim();
        String bodyStr = new String(body, StandardCharsets.ISO_8859_1);

        // lấy file byte
        int headerEndIndex = bodyStr.indexOf("\r\n\r\n");
        if (headerEndIndex == -1) {
            throw new IllegalArgumentException("Invalid multipart/form-data format: Missing header-body separator");
        }

        //lay file byte
        int fileStart = headerEndIndex + 4;
        int fileEnd = bodyStr.lastIndexOf(boundary) - 2; // trừ đi \r\n trước boundary

        if (fileEnd < 0 || fileEnd < fileStart) {
            throw new IllegalArgumentException("Invalid multipart/form-data format: File content not found");
        }

        byte[] fileBytes = Arrays.copyOfRange(body, fileStart, fileEnd);
        String header = bodyStr.substring(0, headerEndIndex);
        String filename = extractFilename(header);
        return new FilePart(filename, fileBytes);
    }

    private String extractFilename(String header) {
        if (header.contains("filename=\"")) {
            String after = header.split("filename=\"")[1];
            return after.split("\"")[0];
        }
        return "upload.bin";
    }
}
