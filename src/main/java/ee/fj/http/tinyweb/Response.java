package ee.fj.http.tinyweb;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;

public interface Response {
    public void setCharset(String charset);

    public void setCharset(Charset charset);

    public void setResponseType(TYPE responseType);

    public StringBuffer getWriter();

    public boolean isHeaderWritten();

    public boolean setHeader(boolean writeSizeToHeader, Response.STATUS status) throws IOException;

    public void stream(Response.TYPE responseType, InputStream in) throws UncheckedIOException;

    public void stream(String imageType, BufferedImage image) throws UncheckedIOException;

    public void stream(String responseType, InputStream in) throws UncheckedIOException;

    public static enum STATUS {
        OK("HTTP/1.1 200 OK"),
        NOT_FOUND("HTTP/1.1 404 Not Found"),
        INTERNAL_SERVER_ERROR("HTTP/1.1 500 Internal Server Error");
        
        public final String val;

        private STATUS(String val) {
            this.val = val;
        }
    }

    public static enum TYPE {
        HTML("text/html;"),
        TEXT("text/plain;"),
        CSS("text/css;"),
        JS("application/javascript;"),
        JSON("application/json;");
        
        public final String val;

        private TYPE(String val) {
            this.val = val;
        }
    }

}