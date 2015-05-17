package lib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
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
	private OutputStreamWriter writer;
	private BufferedReader reader;
	private SSLSocket sslSocket = null;
	public boolean busy = false;
	public boolean connected = false;
	public String public_key;
	private String ip;
	private int port;
	
	public ServerConnection(SSLSocket socket) {
		sslSocket = socket;
		this.connected = startServer();
	}
	
	public ServerConnection(String myip, int myport) {
		ip = myip;
		port = myport;
		try {
			// SSL Socket
			SSLSocketFactory sslsf = (SSLSocketFactory)SSLSocketFactory.getDefault();
			sslSocket = (SSLSocket)sslsf.createSocket(ip, port);
			this.connected = startServer();
			
		} catch (UnknownHostException e) {
			System.err.println("No host found at "+ip+":"+port+".");
			
		} catch (IOException e) {
			System.err.println("No listening host at "+ip+":"+port+".");
		} 
	}
	
	public boolean startServer() {
		try {
			// Create input buffer
			InputStream inputstream = sslSocket.getInputStream();
			InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
			reader = new BufferedReader(inputstreamreader);
	
			// Create output stream
			OutputStream outputStream = sslSocket.getOutputStream();
			writer = new OutputStreamWriter(outputStream); 
			
			return true;
		} catch (Exception err) {
			System.err.println(err);
			return false;
		}
	}
	
	public boolean reconnect() {
		if(sslSocket==null && ip != null)
			try {
				SSLSocketFactory sslsf = (SSLSocketFactory)SSLSocketFactory.getDefault();
				sslSocket = (SSLSocket)sslsf.createSocket(ip, port);
			} catch(Exception er) {
				System.out.println("Fuck");
				return false;
			}
		
		this.close();
		this.connected = startServer();
		
		return connected;
	}
	
	public boolean send(String msg) throws IOException {
		try {
			writer.write(msg + "\n");
			writer.flush();
	
			return true;
		} catch (NullPointerException err) {
			throw new IOException("No Socket");
		}
	}
	
	public String receive() throws IOException {
		try {
			writer.flush();
			return reader.readLine();
		} catch (NullPointerException err) {
			throw new IOException("No Socket");
		}
	}
	
	public String request(String msg) throws IOException  {
		send( msg );
		return receive();
	}
	
	public void close() {
		try{
			this.connected = false;
			this.sslSocket.close();
		}catch(Exception err) {
			//err.printStackTrace();
		}
	}
}