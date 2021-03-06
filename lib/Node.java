package lib;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;

/**
 * Demo Helper
 * 	Adds features for displaying, rendering
 *  and capturing the state for presentation
 *  purposes.
 * 
 * @author Reece Notargiacomo
 * @version May 15th 2015
 * 
 */

public class Node extends Security {
	
	// Default ports and IP Addresses
	protected static int port = 0;
	protected static int dirPort = 9998;
	protected static int bankPort = 9999;
	protected static String sslcert, sslpassword;
	protected static String directorIPAddress = "localhost";
	protected static String bankIPAddress = "localhost";
	protected static String ECENTWALLET_FILE = "ecent.wallet";
	
	// Default Constants
	protected final static int DATA = 0;
	protected final static int ECENT = 1;
	protected final static int PKEY = 1;
	private static int DEFAULT_PAUSE_LENGTH = 2000; // 2 seconds
	private static int SHORT_PAUSE_LENGTH = 200; // 0.2 seconds
	private String NODE_TYPE = null;

	// Analyst subtypes
	protected enum AnalystType { NAV, ORC }
	protected static AnalystType analyst_type;
	
	// For directional analysts
	protected final static char LEFT = '0';
	protected final static char RIGHT = '1';
	protected final static char UP = '2';
	protected final static char DOWN = '3';
	protected final static String[] direction = {"LEFT","RIGHT","UP","DOWN"};

	// For accessing array coordinates
	protected final static int x = 0;
	protected final static int y = 1;
	
	// Colours
	public static boolean show_colours = true;
	public static final String RESET = "\u001B[0m";
	public static final String BLACK = "\u001B[30m";
	public static final String RED = "\u001B[31m";
	public static final String GREEN = "\u001B[32m";
	public static final String YELLOW = "\u001B[33m";
	public static final String BLUE = "\u001B[34m";
	public static final String PURPLE = "\u001B[35m";
	public static final String CYAN = "\u001B[36m";
	public static final String WHITE = "\u001B[37m";
	
	/*
	 *	Load Parameters!
	 */
	public static int load_parameters(String[] args) {
		
		int params_given = 0;

		// If IP Addresses given
		for(String argument : args) {
			String[] arg = argument.split("=");
			
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

			if (arg[0].equals("-port"))
			{
				port = Integer.parseInt(arg[1]);
				params_given++;
			}

			if (arg[0].equals("-wallet") || arg[0].equals("-bankvault"))
			{
				ECENTWALLET_FILE = arg[1];
				params_given++;
			}

			if (arg[0].equals("-sslcert"))
			{
				sslcert = arg[1];
				params_given++;
			}

			if (arg[0].equals("-sslpass"))
			{
				sslpassword = arg[1];
				params_given++;
			}

			if (arg[0].equals("-nocolour"))
			{
				show_colours = false;
				params_given++;
			}

			if (arg[0].equals("-demomode")) {
				DEFAULT_PAUSE_LENGTH *= 2;
				SHORT_PAUSE_LENGTH *= 5;
				params_given++;
			}

			if (arg[0].toUpperCase().equals("NAV")) {
				analyst_type = AnalystType.NAV;
				params_given++;
			}
			if (arg[0].toUpperCase().equals("ORC")) {
				analyst_type = AnalystType.ORC;
				params_given++;
			}
		}
		
		return params_given;
	}
	
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
	
	public static int[] extract_coordinates(String str) {
		try {
			String[] s = str.split(",");
			int[] coord = new int[2];
			coord[x] = Integer.parseInt(s[0]);
			coord[y] = Integer.parseInt(s[1]);
			
			return coord;
		} catch (Exception er) {
			return new int[2];
		}
	}
	
	public static String stringCoords(int x1, int y1, int x2, int y2) {
		return ""+x1+","+y1+":"+x2+","+y2;
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
		System.out.println("\n<< " + colour(this.NODE_TYPE,BLUE) + ": " + state + " >>\n");
	}
	
	public void ALERT (String message) {
		try {
			System.out.println(" >> " + message);
			Thread.sleep( SHORT_PAUSE_LENGTH );
			
		} catch (Exception err) { err.printStackTrace(); }
	}

	public void ALERT_ACTION (String message) {
		try {
			System.out.println("> " + message);
			Thread.sleep( SHORT_PAUSE_LENGTH );
			
		} catch (Exception err) { err.printStackTrace(); }
	}

	public void WARN (String message) {
		try {
			System.out.println(colour(" >> " + message,PURPLE));
			Thread.sleep( SHORT_PAUSE_LENGTH );
			
		} catch (Exception err) { err.printStackTrace(); }
	}
	
	public void ERROR(String message) {
		try {
			System.out.println(colour(" >> " + message,RED));
			Thread.sleep( SHORT_PAUSE_LENGTH );
			
		} catch (Exception err) { err.printStackTrace(); }
	}
	
	public void SUCCESS(String message) {
		try {
			System.out.println(colour(" >> " + message,GREEN));
			Thread.sleep( SHORT_PAUSE_LENGTH );
			
		} catch (Exception err) { err.printStackTrace(); }
	}
	
	public void QUICK_SUCCESS(String message) {
		System.out.println(colour(" >> " + message,GREEN));
	}
	
	
	public void ERROR_WITH_DELAY(String message) {
		try {
			System.out.println(colour(" >> " + message,RED));
			Thread.sleep( DEFAULT_PAUSE_LENGTH );
			
		} catch (Exception err) { err.printStackTrace(); }
	}
	
	public void ALERT_WITH_DELAY (String message) {
		try {
			System.out.println(" >> " + message);
			Thread.sleep( DEFAULT_PAUSE_LENGTH );
			
		} catch (Exception err) { err.printStackTrace(); }
	}
	
	public static String colour(String text, String COLOUR) {
		if (show_colours)
			return new String(COLOUR + text + RESET);

		// Don't show colours
		return text;
	}

	public static int randInt(int min, int max) {
	    Random rand = new Random();
	    int randomNum = rand.nextInt((max - min) + 1) + min;

	    return randomNum;
	}
}