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
	private static final int DATA = 1;
	private static final int ECENT = 2;

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
			String msg_raw;
			while (client.connected && !client.busy) {
				
				if ((msg_raw = client.receive()) != null) {
					Message msg = new Message(msg_raw);
					String[] msg_data = msg.getData();
					
					switch (msg.getFlag()) {
					case MessageFlag.C_INIT:
						// Collector connecting
						ALERT("Collector connected...");
						String data_type_available = "" + analystPool.containsKey(msg.data);
						client.send(data_type_available);

						break;

					case MessageFlag.A_INIT:
						// Analysis connecting
						if (!analystPool.containsKey(msg_data[DATA_TYPE])) {
							HashSet<ServerConnection> set = new HashSet<ServerConnection>();
							set.add(client); // add Host:Port of analyst to
												// hashset
							analystPool.put(msg_data[DATA_TYPE], set); // add
						} else {
							HashSet<ServerConnection> analystpool_data_type = analystPool
									.get(msg_data[DATA_TYPE]);
							analystpool_data_type.add(client);
						}

						ALERT("Analyst connected... (" + msg_data[DATA_TYPE] + ")");

						client.send("REGISTERED");
						client.busy = true;

						break;

					case MessageFlag.EXAM_REQ:
						ALERT("Collector sending request...");
						ALERT("Data Analysis request recieved");
						boolean success = false;

						// getAnalysts of the right data type
						HashSet<ServerConnection> getAnalysts = analystPool.get(msg_data[DATA_TYPE]);
						
						if (getAnalysts == null)
							getAnalysts = new HashSet<ServerConnection>();

						for (ServerConnection analyst : getAnalysts) {
							if (!busyAnalyst.contains(analyst)) {
								ALERT("Analyst found! Analysing...");
								
								// Forward DATA + eCent (without data type)
								String request = msg_data[DATA] + ";" + msg_data[ECENT];
								
								busyAnalyst.add(analyst);

								if (analyst.send(request)) {
									String result;
									if ((result = analyst.receive()) != null) {

										ALERT("Analysis recieved: " + result);
										ALERT("Forwarding to Collector.");

										if (client.send(result)) {
											ALERT("Result returned to Collector");
											success = true;
										}
										else
											ALERT("Unable to return result to Collector!");
									}
									busyAnalyst.remove(analyst);
									break;
								} else {
									ALERT("Analyst crashed after recieving ecent.. trying next one");
									getAnalysts.remove(analyst); // disconnect analyst
								}
							}
						}

						if (success) {
							ALERT("Finished analysis!");
						}else {
							client.send("FAILURE:No analysts currently available!");
							ALERT("ERROR: No analysts currently available");
						} 

						break;

					default:
						ALERT("Unrecognised message.");
						break;

					}
				} else {
					ALERT("Closing connection");
					client.close();
				}
			}
		}
	}

}
