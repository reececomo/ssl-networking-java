import java.io.*;

import javax.net.ssl.*;
import java.security.*;

import lib.*;

/**
 * Bank Class For handling Ecent generation, checking and depositing
 * 
 * @author Reece Notargiacomo, Alexander Popoff-Asotoff,
 *				Jesse Fletcher and Caleb Fetzer
 */

public class Bank extends Node {

	private static int port = 9999;
	private static SSLServerSocket sslserversocket = null;

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
					switch (msg.getFlagEnum()) {
					
						/*
						 * Bank Withdrawal
						 * BANK_WIT => WIT
						 */
						case WIT:
							ANNOUNCE(colour("Collector",PURPLE)+" connected  -->  Withdrawing money");
							
							int amount = Integer.parseInt(msg.data);
							ALERT("Generating " + amount + " eCents!");
							
							for (int i = 0; i < amount; i++)
								client.send(generateEcent());
		
							SUCCESS("Money sent");
							break;
							
							
						/*
						 * Bank Deposit
						 * BANK_DEP => DEP
						 */
						case DEP:
							ANNOUNCE(colour("Analyst",PURPLE)+" connected  -->  Depositing money");
		
							// Check if eCent is in valid eCent set
							if (bankStore.contains(msg.data)) {
								
								ALERT("Depositing valid eCent");
								ALERT("Sending acknowledgement to Analyst!");
								client.send("VALID");
								bankStore.remove(msg.data);
								
							} else {
								
								ALERT("Rejecting invalid eCent");
								client.send("INVALID");
								
							}
							break;
							
						default:
							ALERT("Unexpected input: " + msg.raw());
							break;
		
					}
	
					ALERT("Request finished!\n");
				} catch (IOException err) {
					ALERT("Closing connection: " + colour(err.getMessage(),RED));
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
