package bai_tap_lon.client.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ClientImageConfig {
    private static final Properties props = new Properties();

    static {
        try (InputStream in = ClientImageConfig.class.getResourceAsStream("/config.properties")) {
            if (in != null) props.load(in);
        } catch (IOException ignored) {
        }
        props.putIfAbsent("image.server.host", "localhost");
        props.putIfAbsent("image.server.port", "8080");
    }

    public static String getServerBaseUrl() {
        return "http://" + props.getProperty("image.server.host")
                + ":" + props.getProperty("image.server.port");
    }

    public static String getUploadEndpoint() {
        return getServerBaseUrl() + "/upload";
    }

    // relativePath: "/uploads/abc.png" hoặc "uploads/abc.png"
    public static String buildPublicImageUrl(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return "";
        if (relativePath.startsWith("http://") || relativePath.startsWith("https://")) {
            return relativePath;
        }
        if (!relativePath.startsWith("/")) {
            relativePath = "/" + relativePath;
        }
        return getServerBaseUrl() + relativePath;
    }

    public static int getPort() {
        return Integer.parseInt(props.getProperty("image.server.port"));
    }
}