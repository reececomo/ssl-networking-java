import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.*;

import javax.net.ssl.*;

import lib.*;
import lib.ServerConnection.NodeType;

/**
 * Director Node class
 * 
 * @author Jesse Fletcher, Caleb Fetzer, Reece Notargiacomo, Alexander Popoff-Asotoff
 * @version 5.9.15
 *
 */
public class Director extends Node {

	private static int PORT = 9998;
	private SSLServerSocket director;

	private static final int DATA_TYPE = 0;
	private static final int PUBLIC_KEY = 1;
	public static int con = 0;

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
				newPort = Integer.valueOf(args[0]);
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
		lib.Security.declareServerCert("keystore.jks", "cits3002");
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
			
			ServerConnection currentAnalyst = null;
			HashSet<ServerConnection> currentAnalystPool = null;
			
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
						ALERT(colour("Collector",PURPLE)+" connected...");
						client.nodeType = NodeType.COLLECTOR;
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

							ALERT(colour("Analyst",PURPLE) + " connected... (" + msg_data[DATA_TYPE] + ")");
							client.nodeType = NodeType.ANALYST;
							
							HashSet<ServerConnection> newpool = new HashSet<ServerConnection>();
							newpool.add(client);
							analystPool.put(msg_data[DATA_TYPE], newpool);
							
						} else {
							HashSet<ServerConnection> existingpool = analystPool.get(msg_data[DATA_TYPE]);
							existingpool.add(client);
						}
						
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
						currentAnalystPool = analystPool.get(msg_data[DATA_TYPE]);
						
						// If there are some analysts
						if (currentAnalystPool != null) {
							
							// Try some analyst until you find one that's free and connected
							for (ServerConnection analyst : currentAnalystPool) {
								currentAnalyst = analyst;
								
								if (!busyAnalyst.contains(analyst)) {
									// Reserve the analyst
									busyAnalyst.add(analyst);
									
									ALERT("Analyst found! Sending Collector the analyst public key...");
									String eCent = client.request(MessageFlag.PUB_KEY + ":" + analyst.public_key);
									String data = null,result = null;

									data = client.receive();
										
									// Send eCent and data, and request result
									if(analyst.send(MessageFlag.EXAM_REQ + ":" + eCent)
											&& analyst.send(data))
										result = analyst.receive();
									else {
										if(currentAnalyst!=null) {
											if(busyAnalyst.contains(currentAnalyst))
												busyAnalyst.remove(currentAnalyst);
											
											if(currentAnalystPool!=null && currentAnalystPool.contains(currentAnalyst))
												currentAnalystPool.remove(currentAnalyst);
										}
										break;
									}
									
									SUCCESS("Analysis received! Forwarding to Collector.");

									client.send(result);
									
									ALERT("Returning result to Collector");
									success = true;
										
									busyAnalyst.remove(analyst);
									break;
								}
							}
						}

						if (!success) {
							client.send("Warning: No analysts currently available!");
							WARN("Warning: No analysts currently available!");
						} 

						break;

					default:
						ERROR("Unrecognised message: " + msg.raw());
						break;

					}
					
				} catch(SSLException err) {

					ALERT("Failed connection: "+colour(err.getMessage(),RED));
					client.close();

				} catch(IOException err) {
					
					/*
					 * Make sure to remove the analyst
					 */
					
					ERROR(""+err.getMessage());
				}
			}
		}
	}

}
