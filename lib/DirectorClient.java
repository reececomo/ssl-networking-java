package lib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.net.ssl.SSLSocket;

public class DirectorClient implements Runnable {
 
	private SSLSocket sslsocket;
	private enum Type { UNKNOWN, ANALYST, COLLECTOR };
	private Type type;
	public PrintWriter writer;

	public DirectorClient(SSLSocket socket) {
		this.sslsocket = socket;
		this.type = Type.UNKNOWN;
	}
	
	public void run() {
		BufferedReader reader = null;
		writer = null;
		try {
			reader = new BufferedReader(new InputStreamReader( sslsocket.getInputStream() ));
			writer = new PrintWriter(sslsocket.getOutputStream(), true);

			// The read loop. Code only exits this loop if connection is lost /
			// client disconnects
			while (true) {
				String input = reader.readLine();
				
				if (input == null)
					break;

				Message msg = new Message( input );
				String response = processMessage( msg );
				
				if(response!=null)
					writer.println(response);

			}
		} catch (IOException e) { 
			System.out.println("Client disconnected");
			
		} finally {
			
			try {
				if (reader != null) reader.close();
				if (writer != null) writer.close();
			} catch (IOException e) { throw new RuntimeException(e); }
		}
	}
	
	private String processMessage ( Message msg ) {
		switch(this.type) {
			case ANALYST:
				
				break;
			case COLLECTOR:
				
				break;
				
			default:
				return registerClient(msg);
		}
		
		return null;
	}
	
	private String registerClient( Message msg ) {
		if(msg.getFlag() == "DEC")
			if  ( msg.data.equals("analyst") )
				this.type = Type.ANALYST;
			else
				this.type = Type.COLLECTOR;
		
		return "New "+ type.toString() +" connected!";
	}
 
	 
}
