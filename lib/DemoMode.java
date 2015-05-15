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

public class DemoMode {

	private final static int DEFAULT_PAUSE_LENGTH = 5000; // 5000 milliseconds
	private final static int SHORT_PAUSE_LENGTH = 500; // 5000 milliseconds
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
	
	public static void ALERT_WITH_DELAY (String message) {
		try {
			System.out.println(" >> " + message);
			Thread.sleep( DEFAULT_PAUSE_LENGTH );
			
		} catch (Exception err) { err.printStackTrace(); }
	}
}