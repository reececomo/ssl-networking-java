package lib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

public class InConnection {
	private int port;
	private SSLServerSocket sslserversocket;
	private SSLSocket sslSocket;
	private BufferedReader reader;
	private OutputStreamWriter writer;
	
	public InConnection(SSLServerSocket sslsrvsoc) {
		this.sslserversocket = sslsrvsoc;
		try {
			sslSocket = (SSLSocket)sslserversocket.accept();
			
			InputStream is = sslSocket.getInputStream();
			reader = new BufferedReader( new InputStreamReader(is) );
			writer = new OutputStreamWriter( sslSocket.getOutputStream() );
			
		} catch (IOException err) {
			System.err.println(err.getMessage());
		}
		
	}
	public void close() {
		try{
		this.sslSocket.close();
		}catch(Exception err) {
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
	
	public int getPort() {
		return this.port;
	}
}