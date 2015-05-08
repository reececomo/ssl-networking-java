import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import lib.*;

/**
 * Director Node class
 * 
 * @date May 8th 2015
 * @author Reece Notargiacomo
 * @author ...
 *
 */
public class Director {

	private static final int PORT = 9998;
	private SSLServerSocket director;
	private ArrayList<DirectorClient> clients;
	private boolean socketIsListening = true;

	// main
	public static void main(String[] args) {
		Director myDir = new Director(PORT);
	}

	// constructor
	public Director(int portNo) {

		// SSL Certificate
		SSLHandler.declareServerCert("cits3002_01Keystore", "cits3002");
		ExecutorService executorService = Executors.newFixedThreadPool(100);
		
		clients = new ArrayList<DirectorClient>();

		// Start Server and listen
		if (this.startSocket(portNo))
			while (this.socketIsListening)

				try {

					SSLSocket clientSocket = (SSLSocket) director.accept();
					System.out.println("Connecting...");
					DirectorClient newClient = new DirectorClient(clientSocket);
					clients.add(newClient);
					executorService.execute(newClient);
					
					Thread.sleep(1000);
					
					// Test access to analysts/collectors "writer" methods
					for(DirectorClient client : clients ) {
						System.out.println(" > Client" + client);
						Thread.sleep(1000);
						client.writer.println("SUPERSPRINT\n");
						Thread.sleep(1000);
					}
					
				} catch (IOException err) {

					System.err.println("Error connecting client " + err);
					
				} catch (InterruptedException e) {
					
					e.printStackTrace();
				}

	}

	public boolean startSocket(int portNo) {
		try {
			SSLServerSocketFactory sf;

			sf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
			director = (SSLServerSocket) sf.createServerSocket(portNo);

			System.out.println("Director started on " + getIPAddress() + ":"
					+ portNo);

			return true;

		} catch (Exception error) {
			System.err.println("Director failed to start: " + error);
			System.exit(-1);

			return false;
		}
	}

	public String getIPAddress() {
		try {
			// Get IP Address
			return InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			return "UnknownHost";
		}
	}

}
