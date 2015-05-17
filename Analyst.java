import java.io.*;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import lib.*;

/**
 * Analyst Class for analysing data
 * @author Jesse Fletcher, Caleb Fetzer, Reece Notargiacomo, Alexander Popoff-Asotoff
 * @version 5.9.15
 */

public class Analyst extends Node {
	
	// for accessing parts of a message in an array
	private String outMsg;
	private String inMsg;
	
	private ServerConnection bank, director;
	private Server localhost;

	public static void main(String[] args) {
		load_ip_addresses(args);
		new Analyst();
	}

	public Analyst() {
		set_type("ANALYST");
		SSLHandler.declareDualCert("SSL_Certificate","cits3002");

		// Create local server
		try {
			localhost = new Server();
		} catch (IOException error) {
			ANNOUNCE("Could not create Analyst");
			System.exit(-1);
		}
		
		ANNOUNCE("Connecting to Director");
		
		// Connect to bank and director through abstract class
		bank = new ServerConnection(bankIPAddress, bankPort);
		director = new ServerConnection(directorIPAddress, dirPort);
		
		while(true) {
			if(registerWithDirector())
				this.run();
			else
				ALERT_WITH_DELAY("Retrying connection...");
		}
		
	}

	// Send data type to Director
	private boolean registerWithDirector() {

		ANNOUNCE("Registering availability with Director");
		
		String register_message = MessageFlag.A_INIT + ":" + "DATA" + ";" + getIPAddress() + ";" + Integer.toString(localhost.getPort()) + "\n";
		return director.send(register_message);

	}
	
	private boolean depositMoney(String eCent) {
		
		ALERT("Sending eCent to the bank");
		
		String deposit_request = MessageFlag.BANK_DEP + ":" + eCent;
		return bank.request(deposit_request).equals("VALID");
	}
	
	private void run() {
		String[] message;
		
		while (true) {
			
			InConnection director_in = new InConnection(localhost.socket());
			String request = director_in.receive();
			
			if ((message = decrypt(request)) == null) {
				System.err.println("Could not decrypt message!");
				director_in.close();
				
			} else { 
				// Successful decryption
				ALERT("Receiving payment...");
				if ( depositMoney( message[ ECENT ] )) {
					ALERT("Payment received!");
					String analysis = analyse("PARTY", message[ DATA ] );
					director_in.send( analysis );
					director_in.close();
				} else {
					ALERT("Invalid payment!");
					director_in.send("Invalid eCent!");
					director_in.close();
				}

			}
		}
	}
	
	private String[] decrypt(String encryptedMsg) {
		
		// Perform decryption here
		
		return encryptedMsg.split(";");
	}
	
	private String analyse(String analysisType, String rawdata) {
		
		// analyse data here
		
		if ( analysisType.equals("PARTY") )
			// if allowing for multiple analysis types
			return "WeLikeToParty!";
		else
			return "SomeGiberishDataResult!";
			
	}
	
	
}
