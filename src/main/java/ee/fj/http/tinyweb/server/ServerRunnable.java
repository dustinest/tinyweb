package ee.fj.http.tinyweb.server;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import ee.fj.http.tinyweb.Binder;
import ee.fj.http.tinyweb.Request;
import ee.fj.http.tinyweb.Response;
import ee.fj.http.tinyweb.ServerState;
import ee.fj.http.tinyweb.TinyWebServer;

public class ServerRunnable implements Runnable, TinyWebServer {
	private final Object selectorMutex = new Object();

	private static final Logger logger = Logger.getLogger(ServerRunnable.class.getName());
	private final ExecutorService SERVER_LISTENER = Executors.newSingleThreadExecutor();
	private final Selector selector;
	private final String serverName;
	private final BinderWrapper root;
	private final Consumer<ServerState> serverStateConsumer;
	private final ExecutorService subTasks;
	private ServerState serverState = ServerState.STOP;
	private boolean binded = false;

	public ServerRunnable(String name, int threads, Consumer<ServerState> serverstate, BiConsumer<Request, Response> responseHandler) throws IOException {
		this.serverName = "Server: " + name;
		this.root = new BinderWrapper(responseHandler, null, null);
		this.selector = Selector.open();
		this.serverStateConsumer = serverstate;
		if (threads <= 0) {
			threads = Runtime.getRuntime().availableProcessors() / 2;
			if (threads <= 0) threads = 1;
		}
		this.subTasks = Executors.newFixedThreadPool(threads);
	}

	private void listenAddress(int port, InetAddress address) throws IOException {
		expectState(ServerState.INITIALIZING);
		ServerSocketChannel serverChannel = ServerSocketChannel.open();
		serverChannel.configureBlocking(false);
		if (address == null) {
			logger.log(Level.INFO, "Binding port " + port);
			serverChannel.socket().bind(new InetSocketAddress(port));
		} else {
			logger.log(Level.INFO, "Binding " + address + ":" + port);
			serverChannel.socket().bind(new InetSocketAddress(address, port));
		}
		synchronized (selectorMutex) {
			serverChannel.register(this.selector, SelectionKey.OP_ACCEPT);
			this.binded = true;
		}
	}

	private void listen(int port, InetAddress... address) throws IOException {
		try {
			if (address == null || address.length <= 1) {
				listenAddress(port, address == null || address.length == 0 ? null : address[0]);
				return;
			}
			for (InetAddress a : address) {
				listenAddress(port, a);
			}
		} catch (Throwable e) {
			try {
				this.close();
			} finally {
				sendServerState(ServerState.STOP);
			}
			throw e;
		}
	}

	private void expectState(ServerState expectingState) {
		if (this.serverState == serverState) return;
		throw new IllegalStateException("Server should be in " + expectingState + " state. Instead it state is " + this.serverState);
	}

	private void start() throws BindException {
		expectState(ServerState.INITIALIZING);
		synchronized (selectorMutex) {
			if (!this.binded) {
				throw new BindException("Please specify addresses to listen before running the server.");
			}
		}
		sendServerState(ServerState.RUNNING);
		this.SERVER_LISTENER.submit(this);
	}

	@Override
	public void run() {
		synchronized (selectorMutex) {
			if (this.serverState != ServerState.RUNNING || !this.selector.isOpen()) {
				logger.log(Level.FINE, "Server stopped");
				return;
			}
			try {
				runInternal();
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}
	private void runInternal() {
		try {
			int readyChannels = selector.selectNow();
			if(readyChannels == 0) {
				this.SERVER_LISTENER.submit(this);
				return;
			}

			if (!this.selector.isOpen()) {
				logger.log(Level.INFO, "Shutting down... Status: " + this.serverState);
				return;
			}
			Set<SelectionKey> selectionKeys = selector.selectedKeys();
			Iterator<SelectionKey> selectionKeyIterator = selectionKeys.iterator();
			while(selectionKeyIterator.hasNext()) {
				SelectionKey selectionKey = selectionKeyIterator.next();
				if (this.serverState != ServerState.RUNNING) {
					logger.warning("Server is not running - ignoring");
					continue;
				}
				if (!selectionKey.isAcceptable()) {
					logger.warning("Selected key is not accepting - ignoring");
					continue;
				}
				ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
				SocketChannel socketChannel = serverSocketChannel.accept();
				if (socketChannel == null) {
					logger.warning("Socket channel is null - ignoring");
					continue;
				}
				this.subTasks.submit(new ServerHandler(this.serverName, socketChannel, this.root));
				selectionKeyIterator.remove();
			}

			if (this.serverState == ServerState.RUNNING && this.selector.isOpen()) {
				this.SERVER_LISTENER.submit(this);
			} else {
				logger.log(Level.FINE, "Server stopped");
			}
		} catch (Throwable e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			if (this.serverState != ServerState.RUNNING)
				return;
			this.stop();
		}
	}

	/*
	 * WARNING - Removed try catching itself - possible behaviour change.
	 */
	@Override
	public boolean stop() {
		if (this.serverState == ServerState.SHUTTING_DDOWN || this.serverState == ServerState.STOP) {
			return false;
		}
		sendServerState(ServerState.SHUTTING_DDOWN);
		try {
			this.close();
		} catch (Throwable t) {
			logger.log(Level.WARNING, t.getMessage(), t);
		}
		try {
			logger.log(Level.INFO, "Stopping server...");
			this.SERVER_LISTENER.shutdown();
			while (!this.SERVER_LISTENER.isShutdown()) {
				try {
					if (this.SERVER_LISTENER.awaitTermination(60, TimeUnit.SECONDS))
						continue;
					logger.log(Level.INFO, "Server still not stopped...");
				} catch (InterruptedException e) {
					logger.log(Level.SEVERE, e.getMessage(), e);
				}
			}
			logger.log(Level.INFO, "Server stopped");
		} finally {
			try {
				logger.log(Level.INFO, "Stopping all threads");
				this.subTasks.shutdown();
				while (!this.subTasks.isShutdown()) {
					try {
						if (this.SERVER_LISTENER.awaitTermination(60, TimeUnit.SECONDS))
							continue;
						logger.log(Level.INFO, "All threads still not stopped...");
					} catch (InterruptedException e) {
						logger.log(Level.SEVERE, e.getMessage(), e);
					}
				}
				logger.log(Level.INFO, "Threads stopped");
			} finally {
				logger.log(Level.INFO, "Server completely shut down");
				sendServerState(ServerState.STOP);
			}
		}
		return true;
	}

	private void sendServerState(ServerState serverState) {
		logger.log(Level.INFO, "Changing server state from " + this.serverState + " to " + serverState);
		this.serverState = serverState;
		if (serverStateConsumer == null) return;
		try {
			this.serverStateConsumer.accept(serverState);
		} catch (Throwable e) {
			logger.log(Level.WARNING, "Sending server " + serverState + " state error", e);
		}
	}

	private void close() {
		try {
			logger.log(Level.INFO, "Closing selector...");
			this.selector.close();
			logger.log(Level.INFO, "Closing selector done");
		} catch (Throwable e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	@Override
	public Binder bind(String path, BiConsumer<Request, Response> responseHandler) {
		return this.root.bind(path, responseHandler);
	}

	@Override
	public Optional<String> getPath() {
		return this.root.getPath();
	}

	@Override
	public Optional<String> getAbsolutePath() {
		return this.root.getAbsolutePath();
	}

	@Override
	public TinyWebServer start(int port, InetAddress... address) throws IOException {
		sendServerState(ServerState.INITIALIZING);
		this.listen(port, address);
		this.start();
		return this;
	}
}