package ee.fj.http.tinyweb;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;

public interface TinyWebServer extends Binder {
    TinyWebServer start(int port, InetAddress ... inetAddresses) throws BindException, IOException;
    boolean stop();
}