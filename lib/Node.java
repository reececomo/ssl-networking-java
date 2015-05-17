package lib;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
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

public class Node extends Security {
	
	// Default ports and IP Addresses
	protected static int dirPort = 9998;
	protected static int bankPort = 9999;
	protected static String directorIPAddress = "localhost";
	protected static String bankIPAddress = "localhost";
	
	// Default Constants
	protected final static int DATA = 0;
	protected final static int ECENT = 1;

	private final static int DEFAULT_PAUSE_LENGTH = 2500; // 2500 milliseconds
	private final static int SHORT_PAUSE_LENGTH = 200; // 1.5 second
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
	
	public static int load_ip_addresses(String[] args) {
		
		int params_given = 0;

		// If IP Addresses given
		for(String argument : args) {
			String[] arg = argument.split("=");
			
			if( arg.length == 2 ) {
				if(arg[0].equals("-bank"))
				{
					String[] bankFullAddress = arg[1].split(":");
					bankIPAddress = bankFullAddress[0];
					if (bankFullAddress.length == 2)
						bankPort = Integer.parseInt(bankFullAddress[1]);
					
					params_given++;
				}
				
				if (arg[0].equals("-dir"))
				{
					String[] dirFullAddress = arg[1].split(":");
					directorIPAddress = dirFullAddress[0];
					if (dirFullAddress.length == 2)
						dirPort = Integer.parseInt(dirFullAddress[1]);
					
					params_given++;
				}
			}
		}
		
		return params_given;
	}
	
	public void saveToFile(String fileName,
	  BigInteger mod, BigInteger exp) throws IOException {
	  ObjectOutputStream oout = new ObjectOutputStream( new BufferedOutputStream(new FileOutputStream(fileName)));
	  try {
	    oout.writeObject(mod);
	    oout.writeObject(exp);
	  } catch (Exception e) {
	    throw new IOException("Could not save file to disk! ", e);
	  } finally {
	    oout.close();
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