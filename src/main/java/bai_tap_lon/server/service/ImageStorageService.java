package bai_tap_lon.server.service;

public interface ImageStorageService {
    String save(byte[] bytes, String originalFilename);
}
