import java.io.IOException;
import java.security.*;

import lib.*;

/**
 * Analyst Class for analysing data
 * @author Jesse Fletcher, Caleb Fetzer, Reece Notargiacomo, Alexander Popoff-Asotoff
 * @version 5.9.15
 */

public class Analyst extends Node {
	
	private ServerConnection bank, director;
	
	private String analyst_type = "NAV"; // [navigator] or "ORC" [object response coordinator]
	
	private PrivateKey private_key;
	private PublicKey public_key;

	public static void main(String[] args) {
		load_ip_addresses(args);
		
		new Analyst();
	}

	public Analyst() {
		set_type("ANALYST-"+analyst_type);
		
		this.generateKeyPair();
		
		SSLHandler.declareDualCert("SSL_Certificate","cits3002");
		
		// Connect to bank and director through abstract class
		bank = new ServerConnection(bankIPAddress, bankPort);
		director = new ServerConnection(directorIPAddress, dirPort);
		
		//while(bank.connected && director.connected) {
			if(registerWithDirector()){
				ANNOUNCE("Registered!");
				this.run();
			}
			else
				ALERT_WITH_DELAY("Not connected to Director... retrying...");
		//}
		
	}

	// Send data type to Director
	private boolean registerWithDirector() {
		ANNOUNCE("Registering availability with Director");
		String register_message = MessageFlag.A_INIT + ":" + this.analyst_type + ";" + StringFromKey(this.public_key);
		String response;
		try {
			response = director.request(register_message);
		} catch (IOException err) {
			return false;
		}
		
		return response.equals("REGISTERED");
	}
	
	private boolean depositMoney(String eCent) {
		ALERT("Sending eCent to the bank");
		
		String deposit_request = MessageFlag.BANK_DEP + ":" + eCent;
		String result = null;
		
		try {
			result = bank.request(deposit_request);
		} catch (IOException err) {
			ALERT_WITH_DELAY("Error depositing to bank.");
			return false;
		}
				
		return result.equals("VALID");
	}
	
	private void run() {
		
		while (true) {
			ALERT("Awaiting request...");
			try {
				Message request = new Message(director.receive());
				
				ALERT("Receiving request!");
				
				if(request.getFlag().equals(MessageFlag.EXAM_REQ)) {
					
					String eCent = decrypt(request.data,private_key);
					
					if (eCent == null) {
						ALERT("Error: Could not decrypt message! (" + eCent + ")");
					} else {
						// Successful decryption
						ALERT("Depositing payment!");
						
						if (depositMoney(eCent)) {
							ALERT("Payment deposited!");
	
							ALERT("Analysing...");
							String result = analyse(director.receive());
							
							ALERT("...complete!");
							
							director.send( result );
							ALERT("Analysis sent!");
							
						} else {
							director.send("Error: Could not deposit eCent!");
							ALERT("Error: Could not deposit eCent!");
						}
		
					}
				}

			} catch (IOException err) {
				ALERT("Error: Could not recieve message from Director");
				break;
			}
			
		}
	}
	
	private void generateKeyPair() {
		try {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(2048);
			KeyPair pair = keyGen.generateKeyPair();
			private_key = pair.getPrivate();
			public_key = pair.getPublic();
			
		} catch (NoSuchAlgorithmException e) {
			ANNOUNCE("ERROR: Error generating secure socket keys");
			System.exit(-1);
		}
	}
	
	private String analyse(String rawdata) {
		// Analyse data here
		String[] data = rawdata.split(":");
		
		switch(analyst_type) {
		
			/**
			 *  Navigator Analyst
			 */
			case "NAV":
				int[] start = extract_coordinates(data[0]);
				int[] end = extract_coordinates(data[1]);
				
				// Write code here
				// 	Path-finding (or generating)
				// Access to:
				//  start[x],start[y],end[x],end[y]
				// Output should be a character array:
				char[] instructions = {LEFT,DOWN,DOWN,DOWN,RIGHT,RIGHT,
						RIGHT,DOWN,DOWN,LEFT,DOWN,DOWN};
				
				return new String(instructions);
				
			/**
			 * Object Response Coordinator
			 */
			case "ORC":
				String object = data[0];
				String size = data[1];
				
				if (object.equals("Unicorn") || size.equals("SMALL"))
					return "LAZER";
				else
					return "MOVE";
				
			/**
			 * Invalid analyst given
			 */
			default: // Invalid Analyst Type
				return MessageFlag.ERROR;
		}
	}
}
