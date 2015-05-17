import java.security.*;

import lib.*;

/**
 * Analyst Class for analysing data
 * @author Jesse Fletcher, Caleb Fetzer, Reece Notargiacomo, Alexander Popoff-Asotoff
 * @version 5.9.15
 */

public class Analyst extends Node {
	
	private ServerConnection bank, director;
	
	private String analyst_type;
	private PrivateKey private_key;
	private PublicKey public_key;

	public static void main(String[] args) {
		String type = "DATA";
		int any_special_params = load_ip_addresses(args);
		
		if(args.length-any_special_params > 1)
			type = args[0];
		
		new Analyst(type);
	}

	public Analyst(String analyst_type) {
		set_type("ANALYST-"+analyst_type);
		this.analyst_type = analyst_type;
		
		this.generateKeyPair();
		
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
		// Send "flag:type;public_key"
		String register_message = MessageFlag.A_INIT + ":" + this.analyst_type + ";" + StringFromKey(this.public_key);
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
			ALERT("Awaiting request...");
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
	
	private void generateKeyPair() {
		try {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(2048);
			KeyPair pair = keyGen.generateKeyPair();
			private_key = pair.getPrivate();
			public_key = pair.getPublic();
			
			ALERT(StringFromKey(public_key));
			PublicKey pk2 = KeyFromString(StringFromKey(public_key));
			String message = encrypt("lolwhat",pk2);
			ALERT(message);
			ALERT(decrypt(message,private_key));
			
		} catch (NoSuchAlgorithmException e) {
			ANNOUNCE("ERROR: Error generating secure socket keys");
			System.exit(-1);
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
