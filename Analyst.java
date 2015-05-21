import java.io.IOException;
import java.security.*;

import lib.*;

/**
 * Analyst Class for analysing data
 * @author Jesse Fletcher, Caleb Fetzer, Reece Notargiacomo, Alexander Popoff-Asotoff
 * @version 5.9.15
 */

public class Analyst extends Node {
	
	// The persistent connections
	private SocketConnection bank, director;

	// Analyst subtypes (add here)
	private enum AnalystType { NAV, ORC }
	private AnalystType analyst_type = AnalystType.NAV; // [navigator] or "ORC" [object response coordinator]
	
	// Local keys
	private PrivateKey private_key;
	private PublicKey public_key;

	/*
	 * Main
	 */
	public static void main(String[] args) {
		load_parameters(args);
		new Analyst();
	}
	

	public Analyst() {
		set_type("ANALYST-"+analyst_type.name());
		
		this.generateKeyPair();

		lib.Security.declareClientCert("cacerts.jks");
		
		// Connect to bank and director through abstract class
		bank = new SocketConnection(bankIPAddress, bankPort);
		director = new SocketConnection(directorIPAddress, dirPort);
		
		// Maintain a connection with the director
		director.connect();

		while(true) {
			if(registerWithDirector())
			{
				ANNOUNCE("Registered!");
				this.run();
			} else {
				ALERT_WITH_DELAY("Could not connect to Director...");
				ALERT(colour("Retrying...",BLUE));
				director.reconnect();
			}
		}
		
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
		int attempt = 0;
		String result = null;
		String deposit_request = MessageFlag.BANK_DEP + ":" + eCent;

		bank.connect();

		while(attempt < 3) {
			ALERT("Sending eCent to the bank");
			try {
				result = bank.request(deposit_request);
				break;
			} catch (IOException err) {
				ALERT_WITH_DELAY("Connection error! Retrying...");
				bank.reconnect();
			}

			attempt++;
		}

		bank.close();

		if(result!=null)
			return result.equals("VALID");

		return false;
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
							ERROR("Error: Could not deposit eCent!\n");
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
			case NAV:
				//int[] start = extract_coordinates(data[0]);
				//int[] end = extract_coordinates(data[1]);
				
				// Write code here
				// 	Path-finding (or generating)
				// Access to:
				//  start[x],start[y],end[x],end[y]
				// Output should be a character array:
				char[] instructions = {LEFT,DOWN,DOWN,DOWN,RIGHT,RIGHT,
						RIGHT,DOWN,DOWN,LEFT,DOWN,DOWN};
				
				ALERT("Instructions encoded: " + new String(instructions));
				
				return new String(instructions);
				
			/**
			 * Object Response Coordinator
			 */
			case ORC:
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
