import java.io.*;
import java.util.Random;

import java.util.Arrays;

import lib.*;

/**
 * Collector Class
 * @author Jesse Fletcher, Caleb Fetzer, Reece Notargiacomo, Alexander Popoff-Asotoff
 * @version 5.9.15
 */

public class Collector extends Node {

	private static int dirPort = 9998;
	private static int bankPort = 9999;
	private static String directorIPAddress = "localhost";
	private static String bankIPAddress = "localhost";

	private final static String ECENTWALLET_FILE = "collector.wallet";

	private String inMessage;
	
	private ServerConnection bank, director;

	private ECentWallet eCentWallet; // file for holding ecents
	
	
	/**
	 * Collector
	 */
	public static void main(String[] args) throws IOException {
		load_ip_addresses(args);
		new Collector();
	}
	
	// Collector
	
	public Collector() throws IOException {
		set_type("COLLECTOR");
		SSLHandler.declareClientCert("SSL_Certificate","cits3002");
		
		// Connect to bank and director through abstract class
		bank = new ServerConnection(bankIPAddress, bankPort);
		director = new ServerConnection(directorIPAddress, dirPort);
		
		// Initiate eCentWallet
		this.eCentWallet = new ECentWallet( ECENTWALLET_FILE );
		if (eCentWallet.isEmpty())
			buyMoney();
		ANNOUNCE(eCentWallet.displayBalance());

		if(bank.connected && initiateWithDirector())
			sendDirectorData();
	}


	private void buyMoney(){
		ALERT("Sending Money Withdrawl Request..");

		// Withdraw 100 request
		String withdrawl_request = MessageFlag.BANK_WIT + ":100";
		bank.send(withdrawl_request);
		
		String[] eCentBuffer = new String[100];

		int index = 0;
		while((inMessage = bank.receive()) != null){
		//	System.out.println(index + " = " + inMessage);	
			eCentBuffer[index] = inMessage;
			index++;
		}

		eCentWallet.add(eCentBuffer);
	}
	
	// Returns TRUE iff director can handle data analysis (has analyst(s) available)
	private boolean initiateWithDirector() {
		String connect_director = MessageFlag.C_INIT + ":DATA";
        return director.request(connect_director) != null;
	}

	private void sendDirectorData() throws IOException {
		
		ALERT("Connected! (Director)");
		String temporary_eCent = eCentWallet.remove();

		String data = MessageFlag.EXAM_REQ + ":" + "DATA" + ";" + collect() + ";" + temporary_eCent;

		if(director.send(data)) {
			ALERT("Request sent!");
			ALERT("...");

			// Read response
			String response = director.receive();
			
			ALERT("Response recieved!");
			ALERT("RESULT: " + response);
		} else {
			ALERT("Error: Couldn't send data to Director");
			// Put eCent back in wallet if fucked up
			this.eCentWallet.add( temporary_eCent );
		}

	}

	private String collect(){
		int[] array= new int[10];
		Random rand = new Random();
		for (int i=0; i<10; i++){
			array[i]=rand.nextInt();
		}
		System.out.println(Arrays.toString(array));	
		return Arrays.toString(array);
	}
}
