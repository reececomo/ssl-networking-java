import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.*;

import javax.net.ssl.*;

import lib.*;

/**
 * Director Node class
 * 
 * @author Jesse Fletcher, Caleb Fetzer, Reece Notargiacomo, Alexaner Popoff-Asotoff
 * @version 5.9.15
 *
 */
public class Director extends DemoMode {
	
	private static final int PORT = 9998;
	private SSLServerSocket director;
	
	private static final int DATA_TYPE = 0;
	private static final int ADDR = 1;
	private static final int DATA = 1;
	private static final int ADDR_PORT = 2;
	private static final int ECENT = 2;

	private HashMap<String, HashSet<String>> analystPool;	// explained in constuctor
	private HashSet<String> busyAnalyst;			// as above
	
	private boolean socketIsListening = true;
	
	// main
	public static void main(String[] args) {
		Director myDir = new Director( PORT );
	}
	
	//constructor
	public Director ( int portNo ) {
		set_type("DIRECTOR");
		
		analystPool = new HashMap<String, HashSet<String>>();		// hashmap of data types, storing all [address:socket] of analyst's assoc. with data type
		busyAnalyst = new HashSet<String>();				// busy analysts address pool (all analysts in here are currently busy)

		// SSL Certificate
		SSLHandler.declareDualCert("SSL_Certificate","cits3002");
		ExecutorService executorService = Executors.newFixedThreadPool(100);

		ANNOUNCE("Starting director server");
		
		// Start Server and listen
		if( this.startSocket(portNo) ) {
			
			while(this.socketIsListening){
				try {	
					SSLSocket clientSocket = (SSLSocket) director.accept();
					executorService.execute(new DirectorClient ( clientSocket ));
			
				} catch (IOException err) { System.err.println("Error connecting client " + err); }
			}
		}
	}
	
	public boolean startSocket( int portNo ) {
		try {
			SSLServerSocketFactory sf;
			
			sf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
			director = (SSLServerSocket) sf.createServerSocket( portNo );
			
			ANNOUNCE("Director started on " + getIPAddress());
			
			return true;
			
		} catch (Exception error) {
			System.err.println("Director failed to start: " + error );
			System.exit(-1);
			
			return false;
		}
	}

	public class DirectorClient implements Runnable {

		private String inmessage, outmessage;

		protected SSLSocket sslsocket = null;

		public DirectorClient(SSLSocket socket){
			this.sslsocket = socket;
		}

		public void run() {
			try {
				// IO Stream
				InputStream inputstream = sslsocket.getInputStream();
				InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
				BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
				OutputStream outputstream = sslsocket.getOutputStream();
            	OutputStreamWriter outputstreamwriter = new OutputStreamWriter(outputstream);
		
            	// Read a line
				String message_raw = bufferedreader.readLine();
				
				Message msg = new Message(message_raw);
				String msg_flag = msg.getFlag();
				String[] msg_data = msg.getData();

				if(msg_flag.equals(MessageFlag.C_INIT)){		// Collector init
					ALERT("Collector connected...");

					if(analystPool.containsKey(msg.data)){
						outputstreamwriter.write("TRUE\n");
					}else{
						outputstreamwriter.write("FALSE\n");
					}
					outputstreamwriter.flush();

				}else if(msg_flag.equals(MessageFlag.A_INIT)){	// Analysis init
					ALERT("Analyst connected...");
					
					if( !analystPool.containsKey(msg_data[DATA_TYPE]) ){
						HashSet<String> set = new HashSet<String>();
						set.add(msg_data[ADDR] + ":" + msg_data[ADDR_PORT]);		// add Host:Port of analyst to hashset
						analystPool.put(msg_data[DATA_TYPE], set);	// add analyst to datatype pool and socket set
					}else{
						analystPool.get(msg_data[DATA_TYPE]).add(msg_data[ADDR] + ":" + msg_data[ADDR_PORT]);
					}
				
					ALERT("Analyst initialized (" + msg_data[DATA_TYPE] + ")");

				}else if(msg_flag.equals(MessageFlag.EXAM_REQ)){		// Analysis request (examination)

					HashSet<String> disconnectedAnalyst = new HashSet<String>();

					ALERT("Data Analysis request recieved");

					boolean success = false;
					
					HashSet<String> getAnalysts = analystPool.get(msg_data[DATA_TYPE]);	// get analyst that match data type

					for(String address : getAnalysts){			// for each analyst in data type
						if(!busyAnalyst.contains(address)){		// if their address isn't currently busy
							
							// Forward DATA + eCent (without data type)
							outmessage = msg_data[DATA] + ";" + msg_data[ECENT] + "\n";
							
							if(sendDataToAnalyst(outmessage, address)){
								if(outmessage != null){
									outputstreamwriter.write(outmessage);
									outputstreamwriter.flush();
									ALERT("Result returned to collector");
									success = true;
									break;
								}else 
									ALERT("Analyst crashed after recieving ecent.. trying next one");
							}else{
								disconnectedAnalyst.add(address);	// add analyst to DCed set if connection fails
							}
						}
					}

					if(!success)
						ALERT("ERROR: Analysis could not be completed...");

					if(!disconnectedAnalyst.isEmpty())
						for(String s : disconnectedAnalyst)
							analystPool.get(msg_data[DATA_TYPE]).remove(s);		// remove DCed analyst from pool

				}

				sslsocket.close();

			} catch (IOException e){
           	 	System.out.println("Error listening on port or listening for a connection");
				System.out.println(e.getMessage());
    		
    		}
		}
	
		private boolean sendDataToAnalyst(String message, String address){
			
			String host = address.split(":")[0];
			int port = Integer.parseInt(address.split(":")[1]);

			try{
				SSLSocketFactory sslsf = (SSLSocketFactory)SSLSocketFactory.getDefault();
				SSLSocket sslsocket = (SSLSocket)sslsf.createSocket(host, port);

				InputStream inputstream = sslsocket.getInputStream();
				InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
				BufferedReader bufferedreader = new BufferedReader(inputstreamreader);

				OutputStream outputstream = sslsocket.getOutputStream();
       		     OutputStreamWriter outputstreamwriter = new OutputStreamWriter(outputstream); 

				busyAnalyst.add(address);			// connected so add to busy analysts

				ALERT("Sending Data to Analyst (" + address + ")");

				outputstreamwriter.write(outmessage);		// send message to analyst
				outputstreamwriter.flush();

				outmessage = bufferedreader.readLine(); // get message to forward to collector

				busyAnalyst.remove(address);	
				return true;

			}catch (IOException e) { ALERT("Disconnect: Could not connect to Analyst on " + address + ", trying next one..."); }

			return false;
		
		}
			
	}
	
}
