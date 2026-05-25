package bai_tap_lon.server.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ImageConfig {
    private static final Properties props = new Properties();

    static {
        try (InputStream in = ImageConfig.class.getResourceAsStream("/config.properties")) {
            if (in != null) props.load(in);
        } catch (IOException ignored) {
        }
        props.putIfAbsent("image.server.host", "localhost");
        props.putIfAbsent("image.server.port", "8080");
        props.putIfAbsent("image.upload.dir", "server/uploads/");
    }

    public static String getServerBaseUrl() {
        return "http://" + props.getProperty("image.server.host")
                + ":" + props.getProperty("image.server.port");
    }

    // relativePath: "/uploads/abc.png" hoặc "uploads/abc.png"
    public static String buildPublicImageUrl(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return "";
        if (!relativePath.startsWith("/")) relativePath = "/" + relativePath;
        return getServerBaseUrl() + relativePath;
    }

    public static String getUploadDir() {
        return props.getProperty("image.upload.dir");
    }

    public static int getPort() {
        return Integer.parseInt(props.getProperty("image.server.port"));
    }
}