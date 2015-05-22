import java.io.IOException;
import java.security.*;

import lib.*;
import lib.Message.MessageFlag;

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

		lib.Security.declareClientCert("cacerts.jks");
		
		// Connect to bank and director through abstract class
		bank = new SocketConnection(bankIPAddress, bankPort);
		director = new SocketConnection(directorIPAddress, dirPort);

		// Generate key pair and register with bank
		this.registerKeyPair();
		
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
		String register_message = MessageFlag.INITIATE_ANALYST + ":" + this.analyst_type + ";" + StringFromKey(this.public_key);
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
		String deposit_request = MessageFlag.DEPOSIT + ":" + eCent + ";" + StringFromKey(public_key);

		bank.connect();

		while(attempt < 3) {
			ALERT("Sending eCent to the bank");
			try {
				result = bank.request(deposit_request);
				break;
			} catch (IOException err) {
				ALERT_WITH_DELAY("Connection error! Retrying...");
				registerKeyPair(); // re register keypair
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

				if(request.flag == MessageFlag.VALIDATE_WITH_BANK) {
					director.send(""+registerKeyPair());
				}
				
				if(request.flag == MessageFlag.EXAM_REQ) {
					
					// Decrypt eCent
					String eCent = decrypt(request.data, private_key);

					if (eCent == null) {
						ALERT("Error: Could not decrypt message! (" + eCent + ")");

					} else { // Successful decryption

						ALERT("Depositing payment!");
						if (depositMoney(eCent)) {
							ALERT("Payment deposited!");
							ALERT(colour("Analysing...",BLUE));
							String result = analyse(director.receive());
							
							ALERT_WITH_DELAY("...complete!");
							
							director.send( result );
							ALERT("Analysis sent!\n");
							
						} else {
							director.send("Error: Could not deposit eCent!");
							ERROR("Error: Could not deposit eCent!\n");
						}
		
					}
				}

			} catch (IOException err) {
				ALERT("Error: Could not receive message from Director");
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
			ANNOUNCE("ERROR: Could not generate public/private keys!");
			System.exit(-1);
		}
	}
	
	private boolean registerKeyPair() {
		if(private_key == null)
			generateKeyPair();

		// Connect to bank
		int attempt = 0;
		bank.connect();

		while (attempt < 3) {
			try {
				ANNOUNCE("Attempting to register keypair with Bank!");
				if(bank.send(MessageFlag.KEYPAIR + ":" + StringFromKey(public_key))) {
					if(bank.receive().equals("REGISTERED")) {
					ALERT("Registered with bank!");
						bank.close();
						return true;
					}
				}
				else {
					ALERT_WITH_DELAY("Error connecting to bank... retrying...");
					bank.reconnect();
				}

			} catch (IOException e) {
				ANNOUNCE("ERROR: Error registering keypair with bank!");
				break;
			}
			attempt++;
		}

		// Close program
		ERROR("Could not register with bank.");
		System.exit(-1);
		return false;
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
				return null;
		}
	}
}
