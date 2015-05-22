import java.io.*;
import java.security.PublicKey;

import lib.*;
import lib.Message.MessageFlag;

/**
 * Collector Class
 * @author Reece Notargiacomo, Alexander Popoff-Asotoff and Jesse Fletcher
 */

public class Collector extends Node {

	private ECentWallet eCentWallet;
	
	private SocketConnection bank, director;
	private int xpos = 0, ypos = 0;
	
	public static void main(String[] args) throws IOException {
		ECENTWALLET_FILE = "collector.wallet";
		load_parameters(args);
		new Collector();
	}

	/**
	 * Collector
	 */
	public Collector() throws IOException {
		set_type("COLLECTOR");
		lib.Security.declareClientCert("cacerts.jks");

		ANNOUNCE("Starting up " + colour("Mars Rover",RED) + "...");
		
		// Prepare the connections to bank and director through abstract class
		bank = new SocketConnection(bankIPAddress, bankPort);
		director = new SocketConnection(directorIPAddress, dirPort);
		
		// Initiate eCentWallet
		eCentWallet = new ECentWallet( ECENTWALLET_FILE );
		ANNOUNCE(eCentWallet.displayBalance());

		// Run the main program
		this.runCollector();
	}


	/*
	 *	Mars Rover
	 *		BOT Mode
	 */
	private void runCollector() {

		// Initiate the coordination system
		int[] dest = new int[2];
		String coordinates, newx, newy; 
		xpos = randInt(-100,100);
		ypos = randInt(-100,100);

		while(true) {
			// Random coordinates
			dest[x] = randInt(-100,100);
			dest[y] = randInt(-100,100);

			ANNOUNCE("Current position is "+colour("Latitude: "+xpos+", Longitude: "+ypos,BLUE) +".");
			ANNOUNCE("Requesting navigational analysis...");

			coordinates = stringCoords(xpos,ypos,dest[x],dest[y]);
			char[] movements = analyse_data("NAV", coordinates).toCharArray(); // e.g. [0,1,0,0,1,2,0,3,1,3,2,3,2,1]
			
			SUCCESS("Navigation pathway received!");

			if (movements.length > 0)
				ANNOUNCE("Moving");
			
			for(char move : movements) {
				int next_dir = Character.getNumericValue(move);
				
				// Sense the surroundings
				String surroundings = sensor();

				ALERT_ACTION("Sensor results: "+surroundings);

				if(surroundings.equals("ALL-CLEAR"))
					ALERT_ACTION("No obstruction detected... moving "+colour(direction[next_dir], BLUE) + ".");
				else {
					ALERT_WITH_DELAY(colour("Obstruction detected!",RED));
					String response = analyse_data("ORC", surroundings);

					if (response.equals("MOVE"))
						ALERT_ACTION("Obstruction not dangerous! Running over it "+colour(direction[next_dir], BLUE) + " with caution.");

					else if (response.equals("AVOID")) {
						ALERT_ACTION("Obstruction "+colour("dangerous",RED)+".");
						ALERT_ACTION("Re-calculating path!");
						break;
					}
				}
				
				switch(move) {
					case LEFT: this.xpos--; break;
					case RIGHT:this.xpos++; break;
					case DOWN: this.ypos--; break;
					case UP: this.ypos++; break;
				}
			}

			if(movements.length == 0)
				SUCCESS("Finished all moves, request more...");
		}
	}

	/*
	 * Example sensor method
	 *	 (because not a real robot)
	 */
	private String sensor() {
		// 20/24 chance of receiving all-clear
		int variable = randInt(0,24);

		// Returns a statistically likely outcome
		if(variable >= 4)
			return "ALL-CLEAR";
		else
			switch(variable) {
				case 0:
					return "STICK:SMALL";
				case 1:
					return "STICK:LARGE";
				case 2:
					return "ROCK:LARGE";
				case 3:
					return "ALIEN:UNKNOWN";
				default:
					return "ALL-CLEAR";
			}
			
	}


	private void buyMoney(int amount){

		// Connect to bank
		bank.connect();
		
		ALERT("Sending money withdrawl request..");
		String withdrawl_request = MessageFlag.WITHDRAW + ":" + amount;
		boolean sent = false;
		
		while(!sent)
			if(sent = bank.send(withdrawl_request))
				ALERT("Sent! (Awaiting response)");
			else {
				ERROR_WITH_DELAY("Could not contact bank. Retrying... "+sent);
				bank.reconnect();
			}
		
		String eCent;
		String[] eCentBuffer = new String[amount];
		int index = 0;
		
		while(index < amount) {
			try {
				eCent = bank.receive();
				QUICK_SUCCESS("..."+eCent.substring(0,20));

				eCentBuffer[index++] = eCent;
			} catch (IOException err) {
				ERROR_WITH_DELAY("Connection interrupted. Aborting...");
				break;
			}
		}

		eCentWallet.add(eCentBuffer);
		bank.close();
		SUCCESS("eCents added to wallet!");
	}

	private boolean verifyIdentity(String pubkey) {
		ALERT("Verifying Analyst identity with Bank...");
		bank.connect();
		int attempt = 0;
		String status = "";

		while (attempt < 5) {
			try {
				if(bank.send(MessageFlag.VALID_KEYPAIR + ":" + pubkey)) {
					status = bank.receive();
					break;
				}
			} catch(Exception e) {
				ALERT_WITH_DELAY("Error connecting to bank! Retrying...");
				bank.reconnect();
			}
		}

		bank.close();
		return status.equals("true");
	}

	private String messageBank(MessageFlag bankMsg, String eCent) {
		bank.connect();
		String status = null;

		while (status == null) {
			try {
				if(bank.send(bankMsg + ":" + eCent))
					status = bank.receive();
			} catch(Exception e) {
				ALERT_WITH_DELAY("Error connecting to bank! Retrying...");
				bank.reconnect();
			}
		}

		bank.close();

		return status;
	}

	private String analyse_data(String dataType, String data) {
		boolean eCent_sent = false;
		String analyst_identifier = "";

		while(true) {
			if (eCentWallet.isEmpty())
				buyMoney(100);
			
			// Connect to director
			director.connect();
			
			String temporary_eCent = eCentWallet.remove();


			/**
			 *	Request data analysis
			 */
			try {
				// Send examination request (with the data analysis type)
				ALERT("Sending new data analysis request...");
				Message msg = new Message(director.request(MessageFlag.EXAM_REQ + ":" + dataType));
				
				// Check that a public key was sent back
				switch(msg.flag){
					case PUB_KEY:
						ALERT("Analyst public encryption key received!");
						analyst_identifier = msg.data;
						PublicKey analyst_public_key = KeyFromString(analyst_identifier);

						// Verify the public key with the bank
						if(!verifyIdentity(analyst_identifier)) {
							ERROR("Error: Invalid analyst!");
							break;
						} else {
							// Public key was valid with bank
							SUCCESS("Valid analyst.");

							// Encrypt ecent
							String encrypted_eCent = encrypt(temporary_eCent, analyst_public_key);
							ALERT("Encrypting eCent for analyst ("+encrypted_eCent.substring(0,20)+"...)");

							// send encrypted eCent + data
							ALERT("Sending encrypted eCent!");
							eCent_sent = director.send(encrypted_eCent);
							
							ALERT("Sending data for analysis!");
							director.send(data);

							ALERT(colour("Awaiting response...",BLUE));
							
							Message analysis = new Message (director.receive());
							ALERT("Receiving response...");
							
							if(analysis.flag == MessageFlag.ERROR || analysis.flag == MessageFlag.WARNING)
								throw new IOException(analysis.raw());
							else {
								ALERT("Confirming with bank: " + messageBank(MessageFlag.CONFIRM_DEPOSIT,temporary_eCent + ";" + analyst_identifier));
								ALERT("Response received!");
							}
			
							//close connection
							director.close();
							return analysis.data;
						}
					
					case ERROR:
						throw new IOException(msg.raw());

					case WARNING:
						throw new IOException(colour(msg.raw(),PURPLE));

					default:
						throw new NullPointerException("Unexpected message format! " + msg.raw());

				}

			} catch(IOException err) {
				// Error in sending
				if(eCent_sent) {
					ALERT("Cancelling payment with bank: " + messageBank(MessageFlag.CANCEL_DEPOSIT,temporary_eCent + ";" + analyst_identifier));
					eCent_sent = false;
				}

				this.eCentWallet.add( temporary_eCent );

				ALERT("Could not get data: "+err.getMessage());
				ALERT_WITH_DELAY(colour("Retrying...",PURPLE));
				if(!director.connected)
					director.reconnect();
			}
		}
	}

}
