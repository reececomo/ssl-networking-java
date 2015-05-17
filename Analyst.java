import lib.*;

/**
 * Analyst Class for analysing data
 * @author Jesse Fletcher, Caleb Fetzer, Reece Notargiacomo, Alexander Popoff-Asotoff
 * @version 5.9.15
 */

public class Analyst extends Node {
	
	private ServerConnection bank, director;
	
	private String analyst_type;

	public static void main(String[] args) {
		load_ip_addresses(args);
		new Analyst("DATA");
	}

	public Analyst(String analyst_type) {
		set_type("ANALYST-"+analyst_type);
		this.analyst_type = analyst_type;
		
		SSLHandler.declareDualCert("SSL_Certificate","cits3002");
		
		// Connect to bank and director through abstract class
		bank = new ServerConnection(bankIPAddress, bankPort);
		director = new ServerConnection(directorIPAddress, dirPort);
		
		while(bank.connected && director.connected) {
			if(registerWithDirector()){
				ANNOUNCE("Registered!");
				this.run();
			}
			else
				ALERT_WITH_DELAY("Not connected to Director... retrying...");
		}
		
	}

	// Send data type to Director
	private boolean registerWithDirector() {
		ANNOUNCE("Registering availability with Director");
		
		String register_message = MessageFlag.A_INIT + ":" + this.analyst_type;
		return director.request(register_message).equals("REGISTERED");
	}
	
	private boolean depositMoney(String eCent) {
		ALERT("Sending eCent to the bank");
		
		String deposit_request = MessageFlag.BANK_DEP + ":" + eCent;
		String result;
		
		if ((result = bank.request(deposit_request))!=null)
			return result.equals("VALID");
		else
			return false;
	}
	
	private void run() {
		String[] message;
		
		while (true) {
			ALERT("Idle...");
			
			//InConnection director_in = new InConnection(localhost.socket());
			String request = director.receive();
			ALERT("Receiving request!");
			
			if ((message = decrypt(request)) == null) {
				System.err.println("Could not decrypt message!");
				
			} else { 
				// Successful decryption
				ALERT("Receiving payment...");
				if ( depositMoney( message[ ECENT ] )) {
					ALERT("Payment received!");
					String analysis = analyse("PARTY", message[ DATA ] );
					director.send( analysis );
					ALERT("Analysis sent: "+analysis);
				} else {
					ALERT("Invalid payment!");
					director.send("Invalid eCent!");
				}

			}
			
		}
	}
	
	private String[] decrypt(String encryptedMsg) {
		
		// Perform decryption here
		
		return encryptedMsg.split(";");
	}
	
	private String analyse(String analysisType, String rawdata) {
		
		// Analyse data here
		
		return "SUCCESS";
	}
	
	
}
