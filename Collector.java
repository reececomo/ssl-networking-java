import java.io.*;
import java.security.PublicKey;

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
		lib.Security.declareClientCert("cacerts.jks");
		
		// Connect to bank and director through abstract class
		bank = new ServerConnection(bankIPAddress, bankPort);
		director = new ServerConnection(directorIPAddress, dirPort);
		
		// Initiate eCentWallet
		eCentWallet = new ECentWallet( ECENTWALLET_FILE );
		
		ANNOUNCE(eCentWallet.displayBalance());

		if(bank.connected && initiateWithDirector())
			this.runCollector();
		else
			ALERT("Error: "+bank.connected);
	}
	
	private void runCollector() {
		int[] dest = {12,12};
		String coordinates = stringCoords(xpos,ypos,dest[x],dest[y]); // "0,0:12:12"

		while(true) {
			ANNOUNCE("Requesting navigation analysis...");
			char[] movements = analyse_data("NAV", coordinates).toCharArray(); // e.g. [0,1,0,0,1,2,0,3,1,3,2,3,2,1]
			
			ALERT("Analysis recieved.");
			
			if (movements.length > 0)
				ANNOUNCE("Moving");
			
			for(char move : movements) {
				int dir = Character.getNumericValue(move);
				ALERT(colour("Moving "+direction[dir], BLUE));

				// Do something
				// simulate movement?
				
				// sense surroundings?
				//String action = sensor();
				//If action is lazer, make lazer sound
				//ALERT(action);
				
				// if hit object on next movement run something
				// analyse_data("ORC","Unicorn:SMALL");
				
				switch(move) {
					case LEFT: this.xpos--; break;
					case RIGHT:this.xpos++; break;
					case DOWN: this.ypos--; break;
					case UP: this.ypos++; break;
				}
			}

			SUCCESS("Finished all moves, request more...");
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
			} catch (Exception err) {
				ERROR_WITH_DELAY("Could not send request. Retrying...");
			}
		
		String eCent;
		String[] eCentBuffer = new String[amount];
		int index = 0;
		
		while(index < amount)
			try {
				eCent = bank.receive();
				eCentBuffer[index++] = eCent;
			} catch (IOException err) {
				ERROR_WITH_DELAY("Connection interrupted. Retrying...");
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
				ALERT("Connected! (Director)");
			} catch(IOException err) {
				ERROR_WITH_DELAY("Could not contact director. "+colour("Retrying...",BLUE));
				director.reconnect();
			}
		
        return result != null;
	}

	private String analyse_data(String dataType, String data) {
		while(true) {
			if (eCentWallet.isEmpty())
				buyMoney(100);
			
			String temporary_eCent = eCentWallet.remove();

			try {
				director.send(MessageFlag.EXAM_REQ + ":" + dataType);
				
				// Read response
				String encrypted_msg = director.receive();
				
				Message msg = new Message(encrypted_msg);
				
				switch(msg.getFlag()){
					case MessageFlag.PUB_KEY:
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
						String flag = analysis.getFlag();
						ALERT("Receiving response...");
						
						if(flag == MessageFlag.ERROR || flag == MessageFlag.WARNING)
							throw new IOException(analysis.raw());
						else
							ALERT("Response recieved!");

						return analysis.data;
					
					case MessageFlag.ERROR:
						throw new IOException(msg.raw());

					case MessageFlag.WARNING:
						throw new IOException(colour(msg.raw(),PURPLE));

					default:
						throw new NullPointerException("Unexpected message format!");

				}

			} catch(IOException err) {
				// Error in sending
				this.eCentWallet.add( temporary_eCent );

				ALERT("Could not get data: "+err.getMessage());
				ALERT_WITH_DELAY(colour("Retrying...",PURPLE));

				if(!director.connected)
					director.reconnect();
			}
		}
		
	}

}
