package lib;

import java.io.IOException;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

public class Server {
	private int port;
	private SSLServerSocket sslserversocket;
	
	public Server() throws IOException {
		SSLServerSocketFactory factory = (SSLServerSocketFactory)SSLServerSocketFactory.getDefault();
		sslserversocket = (SSLServerSocket) factory.createServerSocket(0);	// get any avail port (default 0)
		port = sslserversocket.getLocalPort();
	}
	
	public int getPort() {
		return port;
	}
	
	public SSLServerSocket socket() {
		return sslserversocket;
	}
}