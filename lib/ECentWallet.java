package lib;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;



/**
 * Container object for Collector's ECents
 * 
 * @author APA and RC
 */
public class ECentWallet {
	
	private static final String FILE_LOCATION = "ecents.wallet";
	private HashSet<String> eCents; // wallet contents
	private File eCentFile;
	
	public ECentWallet() throws IOException
	{
		eCentFile = new File( FILE_LOCATION );
		eCents = new HashSet<String>();
		loadWallet();
	}
	
	// Add new eCent
	public void add(String eCent, boolean sync) {
		
		if(eCents.contains(eCent)) // If already exists, throw exception
			throw new IllegalArgumentException("eCent already exists your fucked in the head cunt: "+eCent);
		else // Otherwise add the eCent
			eCents.add(eCent);
			
		if(sync)
			this.saveWallet();
	}
	
	public void add(String eCent) {
		
		// if add(eCent), syncronise by default
		this.add(eCent,true);
		
	}
	
	// Add an array of eCents from string[]
	public void add(String[] eCentArray) {
		
		for (String eCent : eCentArray)
			this.add(eCent,false);
			
		this.saveWallet();
	}
	
	public boolean isEmpty() {
		return eCents.size() == 0;
	}
	
	private void loadWallet() throws IOException {
		FileReader fr;
    		String rawLine;
    	
		try {
			// Open file
			fr = new FileReader (eCentFile.getAbsoluteFile());
			BufferedReader br = new BufferedReader(fr);
			
			// Load eCents
		    	while ((rawLine = br.readLine()) != null)
		    		this.add( new ECent(rawLine), false);
		    		
		    	// Close file
		    	fr.close();
		    	
		    	System.out.println("Wallet loaded!");
	    	
		} catch (FileNotFoundException e) {
			System.out.println("No wallet found, creating new wallet.");
		}
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
		// This "foreach" will grab the first element if one exists
		for(String eCent : eCents) {
			String deletedECent = eCent;
			
			eCents.remove(eCent); // delete from wallet
			saveWallet(); // save wallet
			
			return deletedECent;
		}
		return null; //returns null if no eCents
	}

}
