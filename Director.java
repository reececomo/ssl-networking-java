import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.*;

import javax.net.ssl.*;

import lib.*;

/**
 * Director Node class
 * 
 * @author Jesse Fletcher, Caleb Fetzer, Reece Notargiacomo, Alexaner
 *         Popoff-Asotoff
 * @version 5.9.15
 *
 */
public class Director extends Node {

	private static int PORT = 9998;
	private SSLServerSocket director;

	private static final int DATA_TYPE = 0;
	private static final int PUBLIC_KEY = 1;

	private HashMap<String, HashSet<ServerConnection>> analystPool; // explained
																	// in
																	// constuctor
	private HashSet<ServerConnection> busyAnalyst; // as above

	private boolean socketIsListening = true;

	// main
	public static void main(String[] args) {
		int newPort = PORT;

		// Option to give the port as an argument
		if (args.length == 1)
			try {
				newPort = Integer.valueOf(args[0], 10);
			} catch (NumberFormatException er) {
				newPort = PORT;
			}

		new Director(newPort);
	}

	// constructor
	public Director(int portNo) {
		set_type("DIRECTOR");

		analystPool = new HashMap<String, HashSet<ServerConnection>>(); // hashmap
		busyAnalyst = new HashSet<ServerConnection>(); // busy analysts address
														// pool (all analysts in
														// here are currently
														// busy)

		// SSL Certificate
		SSLHandler.declareDualCert("SSL_Certificate", "cits3002");
		ExecutorService executorService = Executors.newFixedThreadPool(100);

		ANNOUNCE("Starting director server");

		// Start Server and listen
		if (this.startSocket(portNo)) {

			while (this.socketIsListening) {
				try {
					SSLSocket clientSocket = (SSLSocket) director.accept();
					executorService.execute(new DirectorClient(clientSocket));

				} catch (IOException err) {
					System.err.println("Error connecting client " + err);
				}
			}
		}
	}

	public boolean startSocket(int portNo) {
		try {
			SSLServerSocketFactory sf;

			sf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
			director = (SSLServerSocket) sf.createServerSocket(portNo);

			ANNOUNCE("Director started on " + getIPAddress() + ":" + portNo);

			return true;

		} catch (Exception error) {
			System.err.println("Director failed to start: " + error);
			System.exit(-1);

			return false;
		}
	}

	public class DirectorClient implements Runnable {

		protected ServerConnection client;

		public DirectorClient(SSLSocket socket) {
			client = new ServerConnection(socket);
		}

		public void run() {
			while (client.connected && !client.busy) {
				
				try {
					Message msg = new Message(client.receive());
					String[] msg_data = msg.getData();
					
					switch (msg.getFlagEnum()) {

					/*
					 * C_INIT
					 * Initiate collector
					 * C_INIT => INIC
					 */
					case INIC:
						// Collector connecting
						ALERT("Collector connected...");
						String data_type_available = "" + analystPool.containsKey(msg.data);
						client.send(data_type_available);

						break;

					/*
					 * A_INIT
					 * Initiate analyst
					 * A_INIT => INIA
					 */
					case INIA:
						if (!analystPool.containsKey(msg_data[DATA_TYPE])) {
							
							HashSet<ServerConnection> newpool = new HashSet<ServerConnection>();
							newpool.add(client);
							analystPool.put(msg_data[DATA_TYPE], newpool);
							
						} else {
							HashSet<ServerConnection> existingpool = analystPool.get(msg_data[DATA_TYPE]);
							existingpool.add(client);
						}

						ALERT("Analyst connected... (" + msg_data[DATA_TYPE] + ")");
						
						client.public_key = msg_data[PUBLIC_KEY];
						client.send("REGISTERED");
						client.busy = true;

						break;

					/*
					 * EXAM_REQ:
					 * Data analysis request
					 * EXAM_REQ => DOIT
					 */
					case DOIT:
						ALERT("Collector sending request...");
						ALERT("Data Analysis request recieved");
						boolean success = false;

						// Get list of analysts for this data type
						HashSet<ServerConnection> datatype_analysts = analystPool.get(msg_data[DATA_TYPE]);
						
						// If there are some analysts
						if (datatype_analysts != null) {
							
							// Try some analyst until you find one that's free and connected
							for (ServerConnection analyst : datatype_analysts) {
								
								if (!busyAnalyst.contains(analyst)) {
									// Reserve the analyst
									busyAnalyst.add(analyst);
									
									ALERT("Analyst found! Sending Collector the analyst public key...");
									String eCent = client.request(MessageFlag.PUB_KEY + ":" + analyst.public_key);
									String data = null,result = null;
									
									data = client.receive();
									
									if (analyst.connected) {
										
										// Send eCent and data, and request result
										analyst.send(MessageFlag.EXAM_REQ + ":" + eCent);
										result = analyst.request(data);
										
										ALERT("Analysis received! Forwarding to Collector.");

										client.send(result);
										
										ALERT("Result returned to Collector");
										success = true;
											
										busyAnalyst.remove(analyst);
										break;
										
									} else {
										ALERT("Analyst crashed after recieving ecent.. trying next one");
										datatype_analysts.remove(analyst); // disconnect analyst
										busyAnalyst.remove(analyst);
									}
								}
							}
						}

						if (success) {
							ALERT("Finished analysis!");
						}else {
							client.send("Error: No analysts currently available!");
							ALERT("Error: No analysts currently available!");
						} 

						break;

					default:
						ALERT("Unrecognised message: " + msg.raw());
						break;

					}
				} catch(IOException err) {
					ALERT("Closing connection");
					client.close();
					break;
				}
			}
		}
	}

}
