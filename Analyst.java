import java.io.IOException;
import java.security.*;

import lib.*;
import lib.Message.MessageFlag;

/**
 * Analyst node (for data analysis)
 *
 * @author Reece Notargiacomo, Alexander Popoff-Asotoff, Jesse Fletcher and Caleb Fetzer 
 * @date 5th May 2015
 *
 */

public class Analyst extends Node {
	
	// The connections to bank and director
	private SocketConnection bank, director;
	// Two-key authentication keys
	private PrivateKey private_key;
	private PublicKey public_key;

	/*
	 * Main
	 */
	public static void main(String[] args) {
		// Analyst type set to Navigator default
		// Supported types are NAV (navigator) and ORC (obstruction response coordinator)
		analyst_type = AnalystType.NAV;
		sslcert = "cacerts.jks";

		// Load the parameters
		load_parameters(args);
		new Analyst();
	}
	

	public Analyst() {
		// Declare the node type for Demo output
		set_type("ANALYST-"+analyst_type.name());

		// Declare the ssl truststore
		lib.Security.declareClientCert(sslcert);
		
		// Connect to bank and director through abstract class
		bank = new SocketConnection(bankIPAddress, bankPort);
		director = new SocketConnection(directorIPAddress, dirPort);

		// Generate authentication keypair and register with bank
		generateAsymmetricKeys();
		registerIdentityWith(bank);
		
		// Start a connection with the director
		director.connect();

		while(true) {
			if(registerWithDirector())
			{
				ANNOUNCE("Registered!");
				this.runAnalyst();
			} else {
				// If connection is lost, retry connection.
				ALERT_WITH_DELAY("Could not connect to Director...");
				ALERT(colour("Retrying...",BLUE));
				director.reconnect();
			}
		}
		
	}
	
	private void runAnalyst() {
		while (true) {
			// Receive infinite connections
			ALERT("Awaiting request...");
			try {
				Message request = new Message(director.receive());
				ALERT("Receiving request!");

				switch(request.flag) {
					/*
					 *	Some node is trying to validate
					 *		your public key encryption
					 */
					case VALIDATE_WITH_BANK:
						director.send( ""+registerIdentityWith(bank) ); //returns "true" or "false"
					break;
				
					/*
					 *	Examination request protocol
					 *	
					 *	1. Receive encrypted eCent (and check for validity)
					 *	2. Deposit eCent into bank
					 *	3. Analyse data
					 *	4. Send result
					 *
					 */
				 	case EXAM_REQ:
						/* 1. Receive encrypted eCent *
						 *  (and check for validity)  */
						String eCent = decrypt(request.data, private_key);
						if (eCent == null)
							ERROR("Error: Could not decrypt message! (" + eCent + ")");
						else
						{
							// Successful decryption (and valid eCent)
							ALERT("Depositing payment!");

							/* 2. Deposit eCent into bank */
							if (deposit(eCent)) {
								ALERT("Payment deposited!");
								ALERT_WITH_DELAY(colour("Analysing...",BLUE));

								/* 3. Analyse data */
								String result = analyse(director.receive());
								SUCCESS("...complete!");
								
								/* 4. Send result */
								director.send( result );
								ALERT("Analysis result sent!\n");
								
							} else {
								director.send("Error: Could not deposit eCent!");
								ERROR("Error: Could not deposit eCent!\n");
							}
						}
					break;
				}

			} catch (IOException err) {
				ALERT("Error receiving message from Director");
				break;
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
	
	// Deposit eCent
	private boolean deposit(String eCent) {
		int attempt = 0;
		String result = null;
		String deposit_request = MessageFlag.DEPOSIT + ":" + eCent + ";" + StringFromKey(public_key);

		bank.connect();

		// Attempt 3 times before giving up
		while(attempt < 3) {
			ALERT("Sending eCent to the bank");
			try {
				result = bank.request(deposit_request);
				break;
			} catch (IOException err) {
				ALERT_WITH_DELAY("Connection error! Retrying...");
				registerIdentityWith(bank); // re register keypair
				bank.reconnect();
			}

			attempt++;
		}

		bank.close();

		if(result!=null)
			return result.equals("VALID");

		return false;
	}

	private void generateAsymmetricKeys() {
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
	
	/*
	 *	Register Keypair
	 *
	 */
	private boolean registerIdentityWith(SocketConnection authorityNode) {

		// Connect to authority
		authorityNode.connect();
		ANNOUNCE("Attempting to register identity with authority!");

		// Keep retrying if connection is broken
		while (true) {
			try {
				// Send public key to authority node
				if(authorityNode.send(MessageFlag.KEYPAIR + ":" + StringFromKey(public_key))) {
					// If the response is "registered",
					//	close connection and return true
					if(authorityNode.receive().equals("REGISTERED")) {
						ALERT("Registered with authority!");
						authorityNode.close();
						return true;
					}
				} else {
					ALERT_WITH_DELAY("Error connecting to authority... "+colour("retrying...",BLUE));
					authorityNode.reconnect();
				}

			} catch (IOException e) {
				ANNOUNCE("ERROR: Error registering identity with authority!");
				break;
			}
		}

		// Close program
		ERROR("Could not register identity with authority. Shutting down...");
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
				//Number of moves to send back
				int num_of_moves = 20;

				char[] instructions = new char[num_of_moves];

				for(int i = 0; i < num_of_moves; i++)
					instructions[i] = (""+randInt(0,3)).charAt(0);

				// Normally you would do some
				//	sort of pathfinding algorithm here.
				
				ALERT("Instructions encoded: " + new String(instructions));
				
				return new String(instructions);
				
			/**
			 * Object Response Coordinator
			 */
			case ORC:
				try {
					String object = data[0];
					String size = data[1];

					// Normally you would do some
					//	kind of image processing/identifying here.

					if(object.equals("STICK")) {
						if (size.equals("SMALL"))
							return "MOVE";
						else
							return "AVOID";
					}
					if(object.equals("ROCK"))
						return "AVOID";

					if(object.equals("ALIEN"))
						return "MOVE";

				} catch (NullPointerException er) {
					return "AVOID";
				}

			/**
			 * Invalid analyst given
			 */
			default: // Invalid Analyst Type
				return null;
		}
	}
}
