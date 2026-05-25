package bai_tap_lon.server.http;

public class FilePart {
    private final byte[] bytes;
    private final String filename;

    public FilePart(String filename, byte[] bytes) {
        this.bytes = bytes;
        this.filename = filename;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public String getFilename() {
        return filename;
    }
}
