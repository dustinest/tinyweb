package ee.fj.http.tinyweb;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import ee.fj.http.tinyweb.server.ServerRunnable;

public class TinyWebServerBuilder {
	private static final String DEFAULT_NAME = "FJ tiny server";

    private String name;
    private int threads;
    private Consumer<ServerState> serverStateChange;
    private BiConsumer<Request, Response> rootHandler;

    public TinyWebServerBuilder name(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("The name should not be empty!");
        this.name = name;
        return this;
    }

    public TinyWebServerBuilder threads(int threads) {
        this.threads = threads;
        return this;
    }

    public TinyWebServerBuilder serverStateChange(Consumer<ServerState> serverStateChange) {
        this.serverStateChange = serverStateChange;
        return this;
    }

    /**
     * @param serverstate to get the state feedback while server is running
     * @param rootHandler handle the root request to manage unknown responses
     * @return TinyWebServer
     * @throws IOException
     */
    public TinyWebServer build(BiConsumer<Request, Response> rootHandler) throws IOException {
        return new ServerRunnable(this.name == null || this.name.isBlank() ? DEFAULT_NAME : this.name, this.threads, serverStateChange, rootHandler);
    }

    /**
     * Helper method to get all the remote addresses available
     * @param callback
     * @throws SocketException
     */
    public static void foreachAvailableAddresses(BiConsumer<NetworkInterface, InetAddress> callback) throws SocketException {
        for (Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces(); networkInterfaces.hasMoreElements();) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();
            for (Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();inetAddresses.hasMoreElements();) {
                InetAddress inetAddress = inetAddresses.nextElement();
                byte[] addresses = inetAddress.getAddress();
                if (addresses.length != 4 || addresses[0] == 127 && addresses[1] == 0 && addresses[2] == 0 && addresses[3] == 1) continue;
                callback.accept(networkInterface, inetAddress);
            }
        }
    }

    public static TinyWebServerBuilder builder() {
    	return new TinyWebServerBuilder();
    }
}