import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.*;

import javax.net.ssl.*;
import java.net.SocketException;

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
		protected ServerConnection selected_analyst;

		public DirectorClient(SSLSocket socket) {
			client = new ServerConnection(socket);
		}

		public void run() {
			while (client.connected && client.waiting) {
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
						ALERT(colour("Analyst",PURPLE) + " connected... (" + msg_data[DATA_TYPE] + ")");
						client.nodeType = NodeType.ANALYST;

						if (!analystPool.containsKey(msg_data[DATA_TYPE])) {
							HashSet<ServerConnection> newpool = new HashSet<ServerConnection>();
							newpool.add(client);
							analystPool.put(msg_data[DATA_TYPE], newpool);
							
						} else {
							HashSet<ServerConnection> existingpool = analystPool.get(msg_data[DATA_TYPE]);
							existingpool.add(client);
						}
						
						client.public_key = msg_data[PUBLIC_KEY];
						client.send("REGISTERED");
						client.waiting = false;

						break;

					/*
					 * DOIT:
					 * Data analysis request
					 * (The main transaction between Cllctr+Anlsyt)
					 */
					case DOIT:
						ANNOUNCE("New collector transaction request...");
						ALERT("Analysis request recieved!");

						// Init success
						boolean success = false;
						String encryptedECent = null,
							data = null,
							request = null,
							result = null;

						// Get list of analysts for this data type
						HashSet<ServerConnection> currentPool = analystPool.get(msg_data[DATA_TYPE]);
						if (currentPool != null) {

							// Try some analyst until you find one that's free and connected
							for (ServerConnection analyst : currentPool) {
								
								if (!analyst.busy && analyst.connected) {
									analyst.busy = true;

									ALERT("Analyst found!");
									ALERT("Sending " + colour("Collector",PURPLE) + " the public encryption key!");

									// Send the analysts public key
									request = MessageFlag.PUB_KEY + ":" + analyst.public_key;
									client.send(request);

									// Recieve an encrypted eCent
									encryptedECent = client.receive();
									data = client.receive();

									ALERT("Recieved eCent and data! Forwarding to analyst.");
										
									// Prepare encrypted eCent with an analysis request
									request = MessageFlag.EXAM_REQ + ":" + encryptedECent;

									// Send request and then send data
									if(analyst.send(request) && analyst.send(data)) {
										ALERT("Awaiting result...");

										try {
											result = analyst.receive();
											ALERT("Analysis received!");

										} catch (IOException disconnected) {
											currentPool.remove(analyst);
											break;
										}
									} else {
										analyst.busy = false;
										break;
									}

									if(client.isConnected() && client.send(result)) {
										SUCCESS("Result sent to collector!");
										success = true;
									} else
										throw new IOException("Collector disconnected!");
										
									analyst.busy = false;
									break;
								}
							}
						}

						if (!success) {
							String warn = "Warning: No analysts currently available!";
							client.send(warn);
							WARN(warn);
						} 

						break;


					/*
					 * No recognised message flag
					 *	handle error
					 */
					default:
						ERROR("Unrecognised message: " + msg.raw());
						break;

					}
					
				} catch(SSLException err) {
					String error_message = err.getMessage();
					ALERT("Failed connection: "+colour(error_message,RED));

					// Close the client connection
					client.close();

					// Make sure an analyst is not busy
					if(selected_analyst!=null)
						selected_analyst.busy = false;

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
