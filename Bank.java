import java.io.*;

import javax.net.ssl.*;
import java.security.*;
import java.util.HashSet;

import lib.*;
import lib.Message.MessageFlag;

/**
 * Bank Class For handling Ecent generation, checking and depositing
 * 
 * @author Reece Notargiacomo, Alexander Popoff-Asotoff,
 *				Jesse Fletcher and Caleb Fetzer
 */

public class Bank extends Node {

	// SSL Server Socket
	private static SSLServerSocket sslserversocket = null;

	// Authenticating analysts
	private static HashSet<String> valid_analysts = null;
	private static HashSet<String> waiting_list = null;

	// Physical copy of the banks money
	private static ECentWallet bankStore;

	/**
	 * Bank Node
	 */
	public static void main(String[] args) throws IOException {

		// Default parameters
		port = 9999;
		sslcert = "keystore.jks";
		sslpassword = "cits3002";
		ECENTWALLET_FILE = "bank.wallet";

		// Load further parameters
		load_parameters(args);

		// Start server
		new Bank( port );
	}

	public Bank(int portNo) throws IOException {
		set_type("BANK");

		// Declare SSL certificate
		lib.Security.declareServerCert(sslcert , sslpassword);

		// Initiate the authentication lists...
		valid_analysts = new HashSet<String>();
		waiting_list = new HashSet<String>();

		// Load the banks money
		bankStore = new ECentWallet( ECENTWALLET_FILE );

		ANNOUNCE("Starting bank server");

		if (startServer(portNo)) {
			ANNOUNCE("Bank started on " + getIPAddress() + ":" + portNo);

			// Accept infinite new connections
			while (true) {
				SSLSocket sslsocket = null;
				try {
					sslsocket = (SSLSocket) sslserversocket.accept();
					new Thread(new clientConnection(sslsocket)).start();

				} catch (IOException e) {
					ERROR("Error connecting client");
				}
			}

		}
	}

	private boolean startServer(int portNo) {
		try {
			// Create a new server
			SSLServerSocketFactory sslssf = (SSLServerSocketFactory) SSLServerSocketFactory .getDefault();
			sslserversocket = (SSLServerSocket) sslssf.createServerSocket( portNo );

			// No errors?
			return true;

		} catch (IOException e) {
			ALERT("Could not create server on port " + portNo);
		}

		return false;
	}

	private class clientConnection implements Runnable {

		protected SocketConnection client;
		String raw_ecent = null;

		public clientConnection(SSLSocket socket) {
			client = new SocketConnection(socket);
		}

		public void run() {
			while (client.connected) {
				try {
					Message msg = new Message(client.receive());
					switch (msg.flag) {

						/*
						 * Requesting authenticity
						 * KEYPAIR
						 */
						case KEYPAIR:
							ALERT(colour("Analyst",PURPLE)+" requesting identification!");
							if(!valid_analysts.contains(msg.data))
								valid_analysts.add(msg.data);
							else
								ALERT("...already verified.");

							//IRL: verify analyst authenticity here

							client.send("REGISTERED");
							SUCCESS("Registered!");
							break;

						/*
						 * Validating encryption
						 * VALID_KEYPAIR
						 */
						case VALID_KEYPAIR:
							ALERT(colour("Collector",PURPLE)+" verifying public key authenticity!");

							ALERT("Collector verifying with bank...");
							if(valid_analysts.contains(msg.data))
								SUCCESS("Successfully verified analyst!");
							else
								ERROR("Could not verify the identity of analyst!");

							client.send(""+valid_analysts.contains(msg.data));
							break;

						/*
						 * Bank Withdrawal
						 * WITHDRAW
						 */
						case WITHDRAW:
							ALERT(colour("Collector",PURPLE)+" connected  (Withdrawing money)");
							
							int amount = Integer.parseInt(msg.data);
							ALERT("Generating " + amount + " eCents!");
							
							for (int i = 0; i < amount; i++)
								client.send(generateEcent());
		
							SUCCESS("Money sent!");
							break;
							
							
						/*
						 * Put eCent on deposit waiting_list
						 * DEPOSIT
						 */
						case DEPOSIT:
							ALERT(colour("Analyst",PURPLE)+" connected  (Depositing money)");
							
							raw_ecent = msg.data.split(";")[0];
		
							// Check if eCent is in valid eCent set
							if (bankStore.contains(raw_ecent)) {
								
								ALERT("Depositing valid eCent");
								ALERT("Sending acknowledgement to Analyst!");
								client.send("VALID");
								waiting_list.add(msg.data);
								
							} else {
								
								ALERT("Rejecting invalid eCent: "+raw_ecent);
								client.send("INVALID");
								
							}
							break;

						/*
						 * Deposit eCent into Analyst account
						 * CONFIRM_DEPOSIT
						 */
						case CONFIRM_DEPOSIT:
							ALERT(colour("Collector",PURPLE)+" confirming transaction!");

							// msg.data = eCent:public_key
							raw_ecent = msg.data.split(";")[0];

							if (waiting_list.contains(msg.data) && bankStore.contains(raw_ecent)) {

								bankStore.remove(raw_ecent);
								waiting_list.remove(msg.data);

								// Deposit eCent
								bankStore.add(msg.data);

								ANNOUNCE("Analyst: " + colour("eCent deposited!",GREEN));
								client.send("SUCCESS");
							} else {
								ERROR("Could not cancel eCent deposit!");
								client.send("ERROR");
							}

							break;

						case CANCEL_DEPOSIT:
							ALERT(colour("Collector",PURPLE)+" cancelling transaction!");

							if (waiting_list.contains(msg.data))
								waiting_list.remove(msg.data);

							ERROR("Transaction cancelled!");
							client.send("SUCCESS");

							break;
							
						default:
							ALERT("Unexpected input: " + msg.raw());
							break;
		
					}
	
				} catch (IOException err) {
					ALERT("Closing connection: " + colour(err.getMessage(),PURPLE));
					client.close();
				}
			}
		}
	}

	private static String generateEcent() {
		String eCent = null;

		while(eCent == null || bankStore.contains(eCent))
			eCent = getSHA256Hash(Integer.toString(bankStore.getBalance()));

		System.out.println("    ..."+eCent.substring(0,20));
		bankStore.add(eCent); // add ecent to vault
		
		return eCent;
	}

	private static String getSHA256Hash(String passwordToHash) {

		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			String salt = getSalt();
			md.update(salt.getBytes());
			byte[] bytes = md.digest(passwordToHash.getBytes());
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < bytes.length; i++) {
				sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
	}

	private static String getSalt() throws NoSuchAlgorithmException {
		SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
		byte[] salt = new byte[16];
		sr.nextBytes(salt);
		return salt.toString();
	}

}
