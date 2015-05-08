import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
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
 *
 */
public class Director {
	
	private static final int PORT = 9998;
	private SSLServerSocket director;
	
	private boolean socketIsListening = true;
	
	// main
	public static void main(String[] args) {
		Director myDir = new Director( PORT );
	}
	
	//constructor
	public Director ( int portNo ) {
		
		// SSL Certificate
		SSLHandler.declareServerCert("cits3002_01Keystore","cits3002");
		ExecutorService executorService = Executors.newFixedThreadPool(100);
		
		// Start Server and listen
		if( this.startSocket(portNo) ) 
			while(this.socketIsListening)
				
			try {
			
			SSLSocket clientSocket = (SSLSocket) director.accept();
			executorService.execute(new DirectorClient ( clientSocket ));
			System.out.println("New client");
			
			} catch (IOException err) {
				
				System.err.println("Error connecting client " + err);
			}
		
	}
	
	public boolean startSocket( int portNo ) {
		try {
			SSLServerSocketFactory sf;
			
			sf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
			director = (SSLServerSocket) sf.createServerSocket( portNo );
			
			System.out.println("Director started on " + getIPAddress() + ":" + portNo );
			
			return true;
			
		} catch (Exception error) {
			System.err.println("Director failed to start: " + error );
			System.exit(-1);
			
			return false;
		}
	}

	
	public String getIPAddress() {
		try {
			//Get IP Address
			return InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			return "UnknownHost";
		}
	}
	
}
