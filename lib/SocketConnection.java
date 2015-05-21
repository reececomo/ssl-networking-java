package lib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * 
 * @author Reece Notargiacomo
 * @date 17th May
 *
 */

public class SocketConnection {

	// Role of
	private enum Type {UNDECLARED, CLIENT, SERVER};
	private Type type = Type.UNDECLARED;

	// Give client nodes a client-type
	public enum ClientType {UNDECLARED, ANALYST, COLLECTOR}
	public ClientType nodeType = ClientType.UNDECLARED;

	// Server input/output
	private PrintWriter writer;
	private BufferedReader reader;
	private SSLSocket sslSocket = null;

	// SocketConnection state information
	public boolean busy = false;
	public boolean connected = false;
	public boolean thread_is_open = true;

	// Analysts get a public key
	public String public_key = null;

	// IP / Port
	private String ip;
	private int port;
	
	/**
	 *	Constructor for CLIENT nodes
	 */
	public SocketConnection (SSLSocket socket) {
		this.type = Type.CLIENT;
		
		sslSocket = socket;
		InetSocketAddress addr = (InetSocketAddress) sslSocket.getRemoteSocketAddress();
		
		// Get IP Address information
		ip = addr.getHostName();
		port = addr.getPort();
		
		// Start the server
		this.connected = connect();
	}
	
	/**
	 *	Constructor for SERVER nodes
	 */
	public SocketConnection (String myip, int myport) {
		this.type = Type.SERVER;
		
		// Get IP Address information
		ip = myip;
		port = myport;
	}
	
	/*
	 * Starts the connection
	 *	
	 */
	public boolean connect() {
		try {

			if(type.equals(Type.SERVER)) {
				// Create Socket
				SSLSocketFactory sslsf = (SSLSocketFactory)SSLSocketFactory.getDefault();
				sslSocket = (SSLSocket)sslsf.createSocket(ip, port);
				sslSocket.startHandshake();
			}
			
			// Input
			InputStream inputstream = sslSocket.getInputStream();
			InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
			reader = new BufferedReader(inputstreamreader);
	
			// Output
			OutputStream outputStream = sslSocket.getOutputStream();
			writer = new PrintWriter(outputStream); 
			
			this.connected = true;

		} catch (IOException e) {
			this.connected = false;
		}

		return connected;
	}
	
	/*
	 *	Send a message!
	 *		returns true/false
	 */
	public boolean send(String msg) {

		if(connected) {
			try {
				// Attempt to send message
				writer.write(msg + "\n");
				writer.flush();
				
				// If connection is broken
				if (writer == null || writer.checkError() == true)
					this.close();
				else
					return true; // Message sent!
				
			} catch (Exception e) { /* handled later */ }
		}

		return false; // Message not sent!
	}
	
	/*
	 *	Recieve a message!
	 *		returns String or throws an error
	 */
	public String receive() throws IOException {
		String input = null;
		if(this.isConnected())  {
			try {
				if ((input = reader.readLine()) == null) {
					// Error/connection closed
					this.close();
					throw new IOException("Connection closed!");
				} else
					return input; // Success!
	
			} catch (NullPointerException err) { }
		}
		
		throw new IOException("Could not connect to "+ip+":"+port);
	}

	/*
	 *	Send a message and expect a response!
	 *		returns String or throws an error
	 */
	public String request(String request) throws IOException {
		if(send(request)) {
			String result = this.receive();
			return result;
		}
			throw new IOException("Lost connection to "+ip+":"+port+"...");
	}

	/*
	 *	Test connection on the fly
	 */
	public boolean isConnected() {
		if(connected)
			connected = writer.checkError() == false;
		
		return connected;
	}
	
	/*
	 *	Reconnect
	 */
	public boolean reconnect() {
		this.close();
		connected = this.connect();
		return connected;
	}
	
	/*
	 *	Close connection
	 */ 
	public void close() {
		try{
			if(connected)
			{ 
				connected = false;
				sslSocket.close();
			}
			sslSocket = null;
			reader = null;
			writer = null;
		} catch(Exception err) { }
	}

}