import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.*;
import java.net.SocketException;

import lib.*;
import lib.Message.MessageFlag;
import lib.SocketConnection.ClientType;

/**
 * Director
 * 
 * @author Reece Notargiacomo, Alexander Popoff-Asotoff,
 *				Jesse Fletcher and Caleb Fetzer
 *
 */
public class Director extends Node {

	// Constants
	private static int port = 9998;
	private static final int DATA_TYPE = 0;
	private static final int PUBLIC_KEY = 1;

	// Analyst Pools are structured by their type of analysis
	private HashMap<String, HashSet<SocketConnection>> analystPools;
	private SSLServerSocket director;

	// Main
	public static void main(String[] args) {
		load_parameters(args);
		new Director( port );
	}

	/*
	 *	Constructor
	 */
	public Director (int portNo) {
		set_type("DIRECTOR");

		// SSL Certificate
		lib.Security.declareServerCert("keystore.jks", "cits3002");

		// Create a new fixed size thread pool
		ExecutorService executorService = Executors.newFixedThreadPool(15);

		ANNOUNCE("Starting director server");
		analystPools = new HashMap<String, HashSet<SocketConnection>>();

		// Start Server and listen
		if (startSocket(portNo)) {

			while (true) {
				try {
					SSLSocket clientSocket = (SSLSocket) director.accept();
					executorService.execute(new DirectorClient(clientSocket));

				} catch (IOException err) {
					System.err.println("Error connecting client " + err);
				}
			}
		}
	}

	/**
	 *	Helper methods
	 */
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

	/**
	 *	Director Client class (implementing Runnable)
	 *
	 */
	public class DirectorClient implements Runnable {

		protected SocketConnection client;
		protected SocketConnection selected_analyst;

		public DirectorClient(SSLSocket socket) {
			client = new SocketConnection(socket);
		}

		public void run() {
			while (client.connected && client.thread_is_open) {
				try {
					Message msg = new Message(client.receive());
					String[] msg_data = msg.getData();
					
					switch (msg.flag) {

					/*
					 * Initiate Analyst
					 */
					case INITIATE_ANALYST:
						ALERT(colour("Analyst",PURPLE) + " connected... (" + msg_data[DATA_TYPE] + ")");
						client.nodeType = ClientType.ANALYST;

						if (!analystPools.containsKey(msg_data[DATA_TYPE])) {
							HashSet<SocketConnection> newpool = new HashSet<SocketConnection>();
							newpool.add(client);
							analystPools.put(msg_data[DATA_TYPE], newpool);
							
						} else {
							HashSet<SocketConnection> existingpool = analystPools.get(msg_data[DATA_TYPE]);
							existingpool.add(client);
						}
						
						client.public_key = msg_data[PUBLIC_KEY];
						client.send("REGISTERED");
						client.thread_is_open = false;

						break;

					/*
					 * DOIT:
					 * Data analysis request
					 * (The main transaction between Cllctr+Anlsyt)
					 */
					case EXAM_REQ:
						client.nodeType = ClientType.COLLECTOR;

						ANNOUNCE("New collector transaction request...");
						ALERT("Analysis request received!");

						// Init success
						boolean success = false;
						String encryptedECent = null,
							data = null,
							request = null,
							result = null,
							warn = null;

						// Get list of analysts for this data type
						HashSet<SocketConnection> currentPool = analystPools.get(msg_data[DATA_TYPE]);
						if (currentPool != null) {

							// Try some analyst until you find one that's free and connected
							for (SocketConnection analyst : currentPool) {
								selected_analyst = analyst;
								
								if (!analyst.busy && analyst.isConnected()) {
									analyst.busy = true;

									ALERT("Analyst found!");
									ALERT("Sending " + colour("Collector",PURPLE) + " the public encryption key!");

									// Tell the analyst to verify with bank (if haven't already)
									if(analyst.send(MessageFlag.VALIDATE_WITH_BANK + ""))
										analyst.receive();

									// Send the analysts public key
									request = MessageFlag.PUB_KEY + ":" + analyst.public_key;
									client.send(request);

									// receive an encrypted eCent
									encryptedECent = client.receive();
									data = client.receive();

									ALERT("received eCent and data! Forwarding to analyst.");
										
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
											warn = "Analyst disconnected during transaction!";
											break;
										}
									} else {
										currentPool.remove(analyst);
										warn = "Analyst disconnected during transaction!";
										break;
									}

									if(client.isConnected() && client.send(result)) {
										QUICK_SUCCESS("Result sent to collector!");
										success = true;
									} else
										throw new IOException("Collector disconnected!");
										
									analyst.busy = false;
									break;
								}
							}
						}

						if (!success) {
							if(warn==null)
								warn = "No analysts currently available!";
							client.send("Warning: "+warn);
							WARN("Warning: "+warn);
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
					
				} catch(SSLException error) {
					// Close the client connection
					client.close();

				} catch(IOException err) {
					
					/*
					 * Make sure to remove the analyst
					 */
					ERROR(""+err.getMessage());
				}

				// Disconnect any busy analysts
				if(selected_analyst!=null)
					selected_analyst.busy = false;
			}
		}
	}

}
