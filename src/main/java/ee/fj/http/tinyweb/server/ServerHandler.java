package ee.fj.http.tinyweb.server;

import java.io.*;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import ee.fj.http.tinyweb.Response;
import ee.fj.http.tinyweb.server.request.RequestHeaderReader;
import ee.fj.http.tinyweb.server.request.RequestHeaders;

class ServerHandler implements Runnable {
	private static final Logger logger = Logger.getLogger(ServerHandler.class.getName());
	private final BinderWrapper root;
	private final SocketChannel socketChannel;
	private final String serverName;

	ServerHandler(String serverName, SocketChannel socketChannel, BinderWrapper root) {
		this.root = root;
		this.socketChannel = socketChannel;
		this.serverName = serverName;
	}

	@Override
	public void run() {
		Socket socket = socketChannel.socket();
		if (socket.isClosed()) {
			logger.log(Level.SEVERE, "It seems like the client already closed the socket");
			return;
		}
		try {
			InputStream in = socket.getInputStream();
			Throwable throwable = null;
			try {
				RequestHeaders header = RequestHeaderReader.getRequestHeaders(in);
				try {
					ServerResponse response = new ServerResponse(new DataOutputStream(socket.getOutputStream()), this.serverName);
					Throwable throwable2 = null;
					try {
						List<String> path = new ArrayList<>(Arrays.asList(header.getPathStream().toArray(String[]::new)));
						this.root.handle(path, header, response, in);
					} catch (Throwable errorInPath) {
						if (!response.isHeaderWritten()) {
							StringWriter stringWriter = new StringWriter();
							errorInPath.printStackTrace(new PrintWriter(stringWriter));
							response.getWriter().append(stringWriter.toString());
							response.setResponseType(Response.TYPE.HTML);
							response.setHeader(true, Response.STATUS.INTERNAL_SERVER_ERROR);
						}
						logger.log(Level.SEVERE, "Error at " + header.getRequestPath().get() + " - " + errorInPath.getMessage(), errorInPath);
						throwable2 = errorInPath;
						throw errorInPath;
					} finally {
						if (throwable2 != null) {
							try {
								response.close();
							} catch (Throwable closingResponse) {
								throwable2.addSuppressed(closingResponse);
							}
						} else {
							response.close();
						}
					}
				} catch (Throwable e) {
					if (header.getRequestPath().isPresent()) {
						logger.log(Level.SEVERE, "Error at " + header.getRequestPath().get() + " - " + e.getMessage(), e);
						return;
					}
					logger.log(Level.SEVERE, "Error at <no path> - " + e.getMessage(), e);
				}
			} catch (Throwable header) {
				throwable = header;
				throw header;
			} finally {
				if (in != null) {
					if (throwable != null) {
						try {
							in.close();
						} catch (Throwable header) {
							throwable.addSuppressed(header);
						}
					} else {
						in.close();
					}
				}
			}
		} catch (Throwable e) {
			logger.log(Level.SEVERE, "Request error" + e.getMessage(), e);
		} finally {
			try {
				socket.close();
			} catch (Throwable e) {
				logger.log(Level.SEVERE, "Socket closing error", e);
			}
			try {
				socketChannel.close();
			} catch (Throwable e) {
				logger.log(Level.SEVERE, "Socket channel closing error", e);
			}
		}
	}
}