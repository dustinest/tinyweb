package ee.fj.http.tinyweb.server.request;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

// TODO: implement this
public class MultipartContent {
    private static final Logger logger = Logger.getLogger(MultipartContent.class.getName());

    private static final String CONTENT_DISPOSITION = "Content-Disposition: ";
    private static final String NAME = "name=";
    private static final String FILENAME = "filename=";
    private static final String CONTENT_TYPE = "Content-Type: ";
    private final String name;
    private final String contentDisposition;
    private final String contentType;
    private final String fileName;
    private final byte[] data;

    MultipartContent(String name, String contentDisposition, String contentType, String fileName, byte[] data) {
        this.name = name;
        this.contentDisposition = contentDisposition;
        this.contentType = contentType;
        this.fileName = fileName;
        this.data = data;
    }

    static MultipartContent getInstance(InputStream in, String boundary) throws IOException {
        boundary = "--" + boundary;
        ISO_8859_1ByteBuffer lineBuffer = new ISO_8859_1ByteBuffer();
        int i = in.read();
        while (i > -1) {
            if (i != 13) {
                if (i == 10) {
                    if (lineBuffer.equals(boundary)) {
                        break;
                    }
                } else {
                    lineBuffer.write(i);
                }
            }
            i = in.read();
        }
        lineBuffer.reset();
        ISO_8859_1ByteBuffer buffer = new ISO_8859_1ByteBuffer();
        return MultipartContent.parse(in, boundary, lineBuffer, buffer);
    }

    static MultipartContent parse(InputStream in, String boundary, ISO_8859_1ByteBuffer lineBuffer, ISO_8859_1ByteBuffer buffer) throws IOException {
        int newLineCount = 0;
        int i = in.read();
        while (i > -1) {
            if (i == 10) {
                if (++newLineCount > 1) break;
                lineBuffer.parseMeaningfulPrefix((prefix, result) -> {
                    if (prefix == null) {
                        System.out.println(lineBuffer.getAsString());
                    } else if (prefix.equals(CONTENT_DISPOSITION) || prefix.equals(CONTENT_TYPE)) {
                        // empty if block
                    }
                    System.out.println(prefix);
                    System.out.println(result);
                }
                , CONTENT_DISPOSITION, CONTENT_TYPE);
                lineBuffer.reset();
            } else if (i != 13) {
                newLineCount = 0;
                lineBuffer.write(i);
            }
            i = in.read();
        }
        System.out.println(newLineCount);
        lineBuffer.reset();
        i = in.read();
        while (i > -1) {
            if (i == 10) {
                if (lineBuffer.equals(boundary)) {
                    System.out.println("DONE");
                    break;
                }
                lineBuffer.reset();
            } else if (i != 13) {
                lineBuffer.write(i);
            }
            buffer.write(i);
            i = in.read();
        }
        while (buffer.endsWith('\n') || buffer.endsWith('\r')) {
            buffer.reduce(1);
        }
        buffer.reduce(lineBuffer.toByteArray().length);
        while (buffer.endsWith('\n') || buffer.endsWith('\r')) {
            buffer.reduce(1);
        }
        System.out.println("[" + buffer.toString() + "]");
        System.out.println("GOT HERE!");
        return null;
    }
}