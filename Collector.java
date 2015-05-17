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

	private int xpos = 0, ypos = 0;
	
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
		eCentWallet = new ECentWallet( ECENTWALLET_FILE );
		
		ANNOUNCE(eCentWallet.displayBalance());

		if(bank.connected && initiateWithDirector())
			this.runCollector();
	}
	
	private void runCollector() {
		int[] dest = {12,12};
		String coordinates = stringCoords(xpos,ypos,dest[x],dest[y]); // "0,0:12:12"
		
		try {
			while(true) {
				// analyse_data(node,coordinates) = String e.g. "01001203132321"
				char[] movements = analyse_data("NAV", coordinates).toCharArray(); // e.g. [0,1,0,0,1,2,0,3,1,3,2,3,2,1]
				
				for(char move : movements) {
					// Do something
					// simulate movement?
					
					// if hit object on next movement run something
					// like --> analyse_data("ORC","Unicorn:SMALL");
					switch(move) {
						case LEFT: this.xpos--; break;
						case RIGHT:this.xpos++; break;
						case DOWN: this.ypos--; break;
						case UP: this.ypos++; break;
					}
					
				}
			}
		} catch(IOException er) {
			
		}
	}

	private String sensor(){
		return "All-Clear";
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

	private String analyse_data(String dataType, String data) throws IOException {
		
		ALERT("Connected! (Director)");
		
		if (eCentWallet.isEmpty())
			buyMoney(100);
		
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
				return analysis.data;
			}
			
		} catch(IOException err) {
			// Error in sending
			ALERT("Error: Connection to Director dropped.");
			this.eCentWallet.add( temporary_eCent );
			throw new IOException("Could not analyse data!");
		}
		
		return null;
	}
}
