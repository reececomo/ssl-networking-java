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

public class ServerConnection {
	private enum ServerType {CLIENT, SERVER};
	public enum NodeType {UNDECLARED, ANALYST, COLLECTOR}
	private ServerType type;
	public NodeType nodeType = NodeType.UNDECLARED;
	private PrintWriter writer;
	private BufferedReader reader;
	private SSLSocket sslSocket = null;
	public boolean busy = false;
	public boolean connected = false;
	public String public_key;
	private String ip;
	private int port;
	
	public ServerConnection(SSLSocket socket) {
		type = ServerType.CLIENT;
		
		sslSocket = socket;
		InetSocketAddress addr = (InetSocketAddress) sslSocket.getRemoteSocketAddress();
		
		ip = addr.getHostName();
		port = addr.getPort();
		
		this.connected = startServer();
	}
	
	public ServerConnection(String myip, int myport) {
		type = ServerType.SERVER;
		
		ip = myip;
		port = myport;
		
		this.connected = startServer();
	}
	
	public boolean startServer() {
		try {
			if(type.equals(ServerType.SERVER)) {
				//create server socket
				SSLSocketFactory sslsf = (SSLSocketFactory)SSLSocketFactory.getDefault();
				sslSocket = (SSLSocket)sslsf.createSocket(ip, port);

				sslSocket.startHandshake();
			}
			
			// Create input buffer
			InputStream inputstream = sslSocket.getInputStream();
			InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
			reader = new BufferedReader(inputstreamreader);
	
			// Create output stream
			OutputStream outputStream = sslSocket.getOutputStream();
			writer = new PrintWriter(outputStream); 
			
			this.connected = true;
			return true;
		} catch (UnknownHostException e) {
			//System.err.println("No host found at "+ip+":"+port+".");
		
		} catch (ConnectException err) {
			//System.err.println("Connection refused "+ip+":"+port+".");
			
		} catch (IOException e) {
			//System.err.println("Connection error: " + e);
		}
		
		this.connected = false;
		return false;
	}
	
	public boolean reconnect() {
		this.close();
		return this.startServer();
	}
	
	public boolean send(String msg) {
		if(this.connected) {
			boolean errors = true;
			try {
				writer.write(msg + "\n");
				writer.flush();
				
				if (writer!=null)
					errors = writer.checkError();
				
			} catch (Exception Err) {
				//System.out.println("Error: "+Err);
				return false;
			}
			
			if(errors)
				this.close();
			
			return errors == false;
		}
			else return false;
	}
	
	public String receive() throws IOException {
		String input = null;
		
		if(this.connected)  {
			try {
				if((input=reader.readLine()) != null)
					return input;
				else {
					this.close();
					throw new IOException("Connection closed"); //null returned
				}
	
			} catch (NullPointerException err) { }
		}
		
		throw new IOException("Could not connect to "+ip+":"+port);
	}
	
	public String request(String msg) throws IOException {
		if(send(msg))
			return receive();
		else
			throw new IOException("Could not connect to "+ip+":"+port);
	}
	
	public void close() {
		try{
			if(this.connected)
			{ 
				this.connected = false;
				this.sslSocket.close();
			}
		}catch(Exception err) {
			err.printStackTrace();
		}
	}
}