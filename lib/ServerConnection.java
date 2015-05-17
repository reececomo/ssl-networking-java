package lib;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

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
	private SSLSocket sslSocket;
	
	public ServerConnection(String ip, int port) {
		try {
		// SSL Socket
		SSLSocketFactory sslsf = (SSLSocketFactory)SSLSocketFactory.getDefault();
		sslSocket = (SSLSocket)sslsf.createSocket(ip, port); 

		// Create input buffer
		InputStream inputstream = sslSocket.getInputStream();
		InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
		reader = new BufferedReader(inputstreamreader);

		// Create output stream
		OutputStream outputStream = sslSocket.getOutputStream();
		writer = new OutputStreamWriter(outputStream); 	
		
		} catch (Exception err) {
			err.printStackTrace();
		}
	}
	
	public boolean send(String msg) {
		try {
			writer.write(msg + "\n");
			writer.flush();
			return true;
		} catch (Exception err) {
			err.printStackTrace();
			return false;
		}
	}
	
	public String receive() {
		try {
			String msg = reader.readLine();
			return msg;
			
		} catch (Exception err) {
			err.printStackTrace();
		}
		return null;
	}
	
	public String request(String msg) {
		send( msg );
		return receive();
	}
	
	public void close() {
		try{
		this.sslSocket.close();
		}catch(Exception err) {
			err.printStackTrace();
		}
	}
}