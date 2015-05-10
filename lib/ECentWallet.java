package lib;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;

/**
 * Abstract Data Type
 * 	To contain and syncronise eCent hash strings
 * 	in volatile memory and to hard disk.
 * 
 * @author Alexander Popoff-Asotoff, Reece Notargiacomo
 * @date May 7th 2015
 * 
 */
public class ECentWallet {
	
	private HashSet<String> eCents; // wallet contents
	private File eCentFile;
	
	public ECentWallet( String myECentFileLocation ) throws IOException
	{
		eCentFile = new File( myECentFileLocation );
		eCents = new HashSet<String>();
		loadWallet(); // load from file (on instantiation)
	}
	
	// Add new eCent (and sync by default)
	public void add(String eCent) { this.add(eCent,true); }
	
	// Add new eCent (with sync option)
	public void add(String eCentHash, boolean sync) {
		if(eCents.contains(eCentHash))
			throw new IllegalArgumentException("Trying to add an eCent already in wallet! eCent hash: " + eCentHash);
		else
			eCents.add(eCentHash); // Add to eCent hashset
			
		if(sync)
			this.saveWallet(); // Sync if requested
	}
	
	// Add an array of eCents from string[]
	public void add(String[] eCentArray) {
		
		for (String eCent : eCentArray)
			this.add(eCent, false); // don't sync until afterwards
			
		this.saveWallet();
	}
	
	// Additional helper methods
	public boolean isEmpty() { return eCents.size() == 0; }
	public int getBalance() { return eCents.size(); }
	public void displayBalance() { System.out.println("You have " + this.getBalance() + " eCents in your wallet!"); }
	
	// Hard disk methods
	private boolean loadWallet() throws IOException {
		try {
			FileReader fr = new FileReader( eCentFile.getAbsoluteFile() ); // Load file
			BufferedReader br = new BufferedReader(fr);
			
			// Load eCents
	    		String loadedECent;
	    		
		    	while ((loadedECent = br.readLine()) != null)
		    		this.add( loadedECent, false);
		    		
		    	fr.close(); //close file
		    	
		} catch (FileNotFoundException e) { return false; }
		
		// if file exists and was loaded, return true
		return true;
	}
	
	private boolean saveWallet() {
		try {
			FileWriter fw = new FileWriter(eCentFile.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			
			bw.write(this.eCentsAsString()); // Dump the contents of the var eCents
			bw.close(); // Save the file
			
			return true;
			
		} catch (IOException error) {
			System.out.println("Could not syncronise eCent Wallet in non-volatile memory!");
			return false;
		}
	}
	
	private String eCentsAsString() {
		StringBuilder myString = new StringBuilder();
		
		for (String eCent : eCents)
			myString.append(eCent + "\n");
			
		return myString.toString();
	}
	
	public String remove() {
		for(String eCent : eCents) {
			String firstECent = eCent;
			
			eCents.remove( eCent ); // delete from wallet
			saveWallet(); // save wallet
			
			return firstECent;
		}
		return null; //returns null if no eCents
	}

}
