package lib;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Demo Helper
 * 	Adds features for displaying, rendering
 *  and capturing the state for presentation
 *  purposes.
 * 
 * @author Reece Notargiacomo
 * @date May 15th 2015
 * 
 */

public class Node {
	
	// Default ports and IP Addresses
	protected static int dirPort = 9998;
	protected static int bankPort = 9999;
	protected static String directorIPAddress = "localhost";
	protected static String bankIPAddress = "localhost";
	
	// Default Constants
	protected final static int DATA = 0;
	protected final static int ECENT = 1;

	private final static int DEFAULT_PAUSE_LENGTH = 2500; // 2500 milliseconds
	private final static int SHORT_PAUSE_LENGTH = 1500; // 1.5 second
	private String STATE = "IDLE";
	private String NODE_TYPE = null;
	
	public void set_type (String node_type) {
		this.NODE_TYPE = node_type;
	}

	public String getIPAddress() {
		try {
			//Get IP Address
			return InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			return "UnknownHost";
		}
	}
	
	public static void load_ip_addresses(String[] args) {

		// If IP Addresses given
		if( args.length == 2 ) {
			String[] bankFullAddress = args[0].split(":");
			String[] dirFullAddress = args[1].split(":");
			
			// Grab the first part
			bankIPAddress = bankFullAddress[0];
			directorIPAddress = dirFullAddress[0];
			
			// Test if ports ALSO given
			if (bankFullAddress.length == 2)
				bankPort = Integer.parseInt(bankFullAddress[1]);
			if (dirFullAddress.length == 2)
				dirPort = Integer.parseInt(dirFullAddress[1]);
		}
	}

	// Announce makes a huge block
	// Alert makes one line
	// Alert with delay makes one line for however many seconds
	public void ANNOUNCE (String state) {
		this.STATE = state;
		System.out.println("\n<< " + this.NODE_TYPE + ": " + this.STATE + " >>\n");
	}
	
	public void ALERT (String message) {
		try {
			System.out.println(" >> " + message);
			Thread.sleep( SHORT_PAUSE_LENGTH );
			
		} catch (Exception err) { err.printStackTrace(); }
	}
	
	public void ALERT_WITH_DELAY (String message) {
		try {
			System.out.println(" >> " + message);
			Thread.sleep( DEFAULT_PAUSE_LENGTH );
			
		} catch (Exception err) { err.printStackTrace(); }
	}
}