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

	private static int port = 9999;
	private static SSLServerSocket sslserversocket = null;
	private static HashSet<String> valid_analysts = null;
	private static HashSet<String> waiting_list = null;

	private static ECentWallet bankStore;

	/**
	 * Bank
	 */
	public static void main(String[] args) throws IOException {
		ECENTWALLET_FILE = "bank.wallet";
		load_parameters(args);
		new Bank( port );
	}

	public Bank(int portNo) throws IOException {
		set_type("BANK");
		lib.Security.declareServerCert("keystore.jks", "cits3002");
		valid_analysts = new HashSet<String>();
		waiting_list = new HashSet<String>();

		bankStore = new ECentWallet(ECENTWALLET_FILE);

		ANNOUNCE("Starting bank server");

		if (startServer(portNo)) {
			ANNOUNCE("Bank started on " + getIPAddress() + ":" + portNo);

			while (true) {
				// Accept new connections
				SSLSocket sslsocket = null;
				try {
					sslsocket = (SSLSocket) sslserversocket.accept();
					ALERT("New connection!");
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

		public clientConnection(SSLSocket socket) {
			client = new SocketConnection(socket);
		}

		public void run() {
			while (client.connected) {
				try {
					Message msg = new Message(client.receive());
					switch (msg.flag) {

						/*
						 * Requesting encryption
						 * BANK_DEP => DEP
						 */
						case KEYPAIR:
							ANNOUNCE(colour("Analyst",PURPLE)+" requesting encryption!");
							valid_analysts.add(msg.data);

							//IRL: verify analyst authenticity here

							client.send("REGISTERED");
							SUCCESS("Registered!");
							break;

						/*
						 * Requesting encryption
						 * BANK_DEP => DEP
						 */
						case VALID_KEYPAIR:
							ANNOUNCE(colour("Collector",PURPLE)+" verifying public key authenticity!");

							ALERT("Collector verifying with bank");

							client.send(""+valid_analysts.contains(msg.data));
							break;

						/*
						 * Bank Withdrawal
						 * BANK_WIT => WIT
						 */
						case WITHDRAW:
							ANNOUNCE(colour("Collector",PURPLE)+" connected  (Withdrawing money)");
							
							int amount = Integer.parseInt(msg.data);
							ALERT("Generating " + amount + " eCents!");
							
							for (int i = 0; i < amount; i++)
								client.send(generateEcent());
		
							SUCCESS("Money sent!");
							break;
							
							
						/*
						 * Bank Deposit
						 * BANK_DEP => DEP
						 */
						case DEPOSIT:
							ANNOUNCE(colour("Analyst",PURPLE)+" connected  (Depositing money)");
		
							// Check if eCent is in valid eCent set
							if (bankStore.contains(msg.data)) {
								
								ALERT("Depositing valid eCent");
								ALERT("Sending acknowledgement to Analyst!");
								client.send("VALID");
								waiting_list.add(msg.data);
								
							} else {
								
								ALERT("Rejecting invalid eCent: "+msg.data);
								client.send("INVALID");
								
							}
							break;

						case CONFIRM_DEPOSIT:
							ANNOUNCE(colour("Collector",PURPLE)+" confirming transaction!");

							if (waiting_list.contains(msg.data) && bankStore.contains(msg.data)) {
								bankStore.remove(msg.data);
								waiting_list.remove(msg.data);

								SUCCESS("eCent deposited!");
								client.send("SUCCESS");
							} else {
								ERROR("Could not cancel eCent deposit!");
								client.send("ERROR");
							}

							break;

						case CANCEL_DEPOSIT:
							ANNOUNCE(colour("Collector",PURPLE)+" cancelling transaction!");

							if (waiting_list.contains(msg.data))
								waiting_list.remove(msg.data);

							ERROR("Transaction cancelled!");
							client.send("SUCCESS");

							break;
							
						default:
							ALERT("Unexpected input: " + msg.raw());
							break;
		
					}
	
					ALERT("Done!");
				} catch (IOException err) {
					ALERT("Closing connection: " + colour(err.getMessage(),RED) + "\n");
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
