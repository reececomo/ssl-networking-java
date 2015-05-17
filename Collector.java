import java.io.*;
import java.security.PublicKey;
import java.util.Random;
import java.util.Arrays;

import lib.*;

/**
 * Collector Class
 * @author Jesse Fletcher, Caleb Fetzer, Reece Notargiacomo, Alexander Popoff-Asotoff
 * @version 5.9.15
 */

public class Collector extends Node {

	private ECentWallet eCentWallet; // file for holding ecents
	private final static String ECENTWALLET_FILE = "collector.wallet";
	
	private ServerConnection bank, director;

	
	public static void main(String[] args) throws IOException {
		load_ip_addresses(args);
		new Collector();
	}

	/**
	 * Collector
	 */
	public Collector() throws IOException {
		set_type("COLLECTOR");
		SSLHandler.declareClientCert("SSL_Certificate","cits3002");
		
		// Connect to bank and director through abstract class
		bank = new ServerConnection(bankIPAddress, bankPort);
		director = new ServerConnection(directorIPAddress, dirPort);
		
		// Initiate eCentWallet
		this.eCentWallet = new ECentWallet( ECENTWALLET_FILE );
		if (eCentWallet.isEmpty())
			buyMoney(100);
		ANNOUNCE(eCentWallet.displayBalance());

		if(bank.connected && initiateWithDirector())
			sendDirectorData("DATA",collect());
	}


	private void buyMoney(int amount){
		
		ALERT("Sending Money Withdrawl Request..");
		String withdrawl_request = MessageFlag.BANK_WIT + ":" + amount;
		boolean sent = false;
		
		while(!sent)
			try {
				sent = bank.send(withdrawl_request);
			} catch (IOException err) {
				ALERT_WITH_DELAY("Could not send request. Retrying...");
			}
		
		String eCent;
		String[] eCentBuffer = new String[amount];
		int index = 0;
		
		while(index < amount)
			try {
				eCent = bank.receive();
				eCentBuffer[index++] = eCent;
			} catch (IOException err) {
				ALERT_WITH_DELAY("Connection interrupted. Retrying...");
				bank.reconnect();
			}

		eCentWallet.add(eCentBuffer);
	}

	private boolean initiateWithDirector()
	{
		String connect_director = MessageFlag.C_INIT + ":DATA";
		String result = null;
		
		while (result == null)
			try {
				result = director.request(connect_director);
			} catch(IOException err) {
				ALERT_WITH_DELAY("Could not contact director. Retrying...");
				director.reconnect();
			}
		
        return result != null;
	}

	private void sendDirectorData(String dataType, String data) {
		
		ALERT("Connected! (Director)");
		String temporary_eCent = eCentWallet.remove();

		try {
			director.send(MessageFlag.EXAM_REQ + ":" + dataType);
			
			ANNOUNCE("Request sent!");
			
			ALERT("Awaiting response/encryption key...");

			// Read response
			String encrypted_msg = director.receive();
			Message msg = new Message(encrypted_msg);
			
			if(msg.getFlag() == MessageFlag.PUB_KEY) {
				PublicKey analyst_public_key = KeyFromString(msg.data);
				ALERT("Public key recieved!");
				
				ALERT("Encrypting eCent!");
				String encrypted_eCent = encrypt(temporary_eCent, analyst_public_key);

				// send encrypted eCent + data
				ALERT("Sending eCent!");
				director.send(encrypted_eCent);
				
				ALERT("Sending data!");
				director.send(data);
				
				Message analysis = new Message (director.receive());
				ALERT("Receiving response...");
				
				if(analysis.getFlag() == MessageFlag.ERROR)
					throw new IOException("Error processing!");
				
				ALERT("Response recieved!");
				ANNOUNCE("RESULT: " + analysis.data);
			}
			
		} catch(IOException err) {
			// Error in sending
			ALERT("Error: Connection to Director dropped.");
			this.eCentWallet.add( temporary_eCent );
		}

	}

	private String collect(){
		int[] array= new int[10];
		Random rand = new Random();
		for (int i=0; i<10; i++){
			array[i]=rand.nextInt();
		}
		System.out.println(Arrays.toString(array));	
		return Arrays.toString(array);
	}
}
