package ee.fj.http.tinyweb.server;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;

import ee.fj.http.tinyweb.Response;

class ServerResponse  implements Response,  Closeable {
    private static final String NEW_LINE = "\r\n";
    private static final String CONNECTION_CLOSE = "Connection: Closed";
    private final DataOutputStream out;
    private final String serverName;
    private Response.TYPE type = Response.TYPE.HTML;
    private String strType = null;
    private Charset charset = Charset.forName("utf-8");
    private boolean isHeaderWritten = false;
    private StringBuffer buffer;

    ServerResponse(DataOutputStream out, String serverName) {
        this.out = out;
        this.serverName = serverName;
    }

    @Override
    public void setCharset(String charset) {
        this.setCharset(Charset.forName(charset));
    }

    @Override
    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    @Override
    public void setResponseType(Response.TYPE responseType) {
        this.type = responseType;
    }

    @Override
    public synchronized StringBuffer getWriter() {
        if (this.buffer == null) {
            this.buffer = new StringBuffer();
        }
        return this.buffer;
    }

    @Override
    public void close() throws IOException {
        try {
            this.setHeader(true, Response.STATUS.OK);
        } finally {
            try {
                if (this.buffer != null) {
                    byte[] b = this.buffer.toString().getBytes(StandardCharsets.UTF_8);
                    this.out.write(b, 0, b.length);
                }
            }
            finally {
                try {
                    this.out.flush();
                }
                finally {
                    this.out.close();
                }
            }
        }
    }

    @Override
    public boolean isHeaderWritten() {
        return isHeaderWritten;
    }

    @Override
    public boolean setHeader(boolean writeSizeToHeader, Response.STATUS status) throws IOException {
        if (this.isHeaderWritten) {
            return false;
        }
        this.isHeaderWritten = true;
        this.out.writeBytes(status.val);
        this.out.writeBytes(NEW_LINE);
        this.out.writeBytes(HEADER.TYPE.val);
        this.out.writeBytes(this.strType != null ? this.strType : this.type.val + HEADER.CHARSET.val + this.charset.displayName());
        this.out.writeBytes(NEW_LINE);
        this.out.writeBytes(this.serverName);
        this.out.writeBytes(NEW_LINE);
        if (writeSizeToHeader && this.buffer != null) {
            this.out.writeBytes(HEADER.LENGTH.val + this.buffer.toString().getBytes(StandardCharsets.UTF_8).length);
            this.out.writeBytes(NEW_LINE);
        }
        this.out.writeBytes(CONNECTION_CLOSE);
        this.out.writeBytes(NEW_LINE);
        this.out.writeBytes(NEW_LINE);
        return true;
    }

    @Override
    public void stream(Response.TYPE responseType, InputStream in) throws UncheckedIOException {
        this.setResponseType(type);
        try {
            BufferedReader inn = new BufferedReader(new InputStreamReader(in));
            Throwable throwable = null;
            try {
                this.setHeader(false, Response.STATUS.OK);
                StringBuffer writer = this.getWriter();
                char[] b = new char[1024];
                int len = inn.read(b);
                while (len != -1) {
                    writer.append(b, 0, len);
                    len = inn.read(b);
                }
            }
            catch (Throwable writer) {
                throwable = writer;
                throw writer;
            } finally {
                if (throwable != null) {
                    try {
                        inn.close();
                    }
                    catch (Throwable writer) {
                        throwable.addSuppressed(writer);
                    }
                } else {
                    inn.close();
                }
            }
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void stream(String imageType, BufferedImage image) throws UncheckedIOException {
        this.strType = "image/" + type;
        try {
            this.setHeader(false, Response.STATUS.OK);
            ImageIO.write(image, imageType, this.out);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void stream(String type, InputStream in) throws UncheckedIOException {
        this.strType = type;
        try {
            Throwable throwable = null;
            try {
                this.setHeader(false, Response.STATUS.OK);
                byte[] b = new byte[1024];
                int len = in.read(b);
                while (len != -1) {
                    this.out.write(b, 0, len);
                    len = in.read(b);
                }
            }
            catch (Throwable b) {
                throwable = b;
                throw b;
            }
            finally {
                if (in != null) {
                    if (throwable != null) {
                        try {
                            in.close();
                        }
                        catch (Throwable b) {
                            throwable.addSuppressed(b);
                        }
                    } else {
                        in.close();
                    }
                }
            }
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private enum HEADER {
        TYPE("Content-Type: "),
        LENGTH("Content-Length: "),
        CHARSET("charset=");

        private final String val;

        HEADER(String val) {
            this.val = val;
        }
    }

}