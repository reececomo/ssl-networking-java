import java.io.*;

import javax.net.ssl.*;

import java.security.*;

import lib.*;

/**
 * Bank Class For handling Ecent generation, checking and depositing
 * 
 * @author Jesse Fletcher, Caleb Fetzer, Reece Notargiacomo, Alexander
 *         Popoff-Asotoff
 * @version 5.9.15
 */

public class Bank extends Node {

	private static int bankPort = 9999;
	private static int sequence = 0;
	private static SSLServerSocket sslserversocket = null;

	private static ECentWallet bankStore;
	private final static String ECENTWALLET_FILE = "bank.wallet";

	/**
	 * Bank
	 */

	public static void main(String[] args) throws IOException {
		// Option to give the port as an argument
		if (args.length == 1)
			try {
				bankPort = Integer.valueOf(args[0]);
			} catch (NumberFormatException er) {
				bankPort = 9999;
			}

		new Bank();
	}

	public Bank() throws IOException {
		set_type("BANK");
		SSLHandler.declareServerCert("SSL_Certificate", "cits3002");

		bankStore = new ECentWallet(ECENTWALLET_FILE);

		ANNOUNCE("Starting bank server");

		if (this.startServer()) {
			ANNOUNCE("Bank started on " + getIPAddress() + ":" + bankPort);
			while (true) {
				SSLSocket sslsocket = null;
				try {
					sslsocket = (SSLSocket) sslserversocket.accept();
					ALERT("Accepting a connection!");

				} catch (IOException e) {
					System.out.println("Error connecting client");
				}

				new Thread(new bankConnection(sslsocket)).start(); // start new
																	// thread
			}
		}
	}

	private boolean startServer() {
		try {
			// Use the SSLSSFactory to create a SSLServerSocket to create a
			// SSLSocket
			SSLServerSocketFactory sslserversocketfactory = (SSLServerSocketFactory) SSLServerSocketFactory
					.getDefault();
			sslserversocket = (SSLServerSocket) sslserversocketfactory
					.createServerSocket(bankPort);
			return true;
		} catch (IOException e) {
			ALERT("Could not create server on port " + bankPort);
		}
		return false;
	}

	private class bankConnection implements Runnable {

		protected ServerConnection client;

		public bankConnection(SSLSocket socket) {
			client = new ServerConnection(socket);
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
							ALERT("Collector connected  -->  Withdrawing money");
							
							int amount = Integer.parseInt(msg.data);
							ALERT("Generating " + amount + " eCents!");
							
							for (int i = 0; i < amount; i++)
								client.send(generateEcent());
		
							ALERT("Money sent");
							break;
							
							
						/*
						 * Bank Deposit
						 * BANK_DEP => DEP
						 */
						case DEP:
							ALERT("Analyst connected  -->  Depositing money");
		
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
	
					ALERT("Request finished!");
				} catch(IOException err) {
					ALERT("Closing connection");
					client.close();
				}
			}
		}
	}

	private static String generateEcent() {
		String eCent = getSHA256Hash(Integer.toString(sequence++));

		bankStore.add(eCent); // add ecent to valid set
		
		return eCent;
	}

	private static String getSHA256Hash(String passwordToHash) {

		String generatedPassword = null;
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			String salt = getSalt();
			md.update(salt.getBytes());
			byte[] bytes = md.digest(passwordToHash.getBytes());
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < bytes.length; i++) {
				sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16)
						.substring(1));
			}
			generatedPassword = sb.toString();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return generatedPassword; // return ecent
	}

	private static String getSalt() throws NoSuchAlgorithmException {
		SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
		byte[] salt = new byte[16];
		sr.nextBytes(salt);
		return salt.toString();
	}

}
