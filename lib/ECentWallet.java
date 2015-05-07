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
	private HashMap<Integer,String> eCents; // wallet contents
	private File eCentFile;
	
	public ECentWallet() throws IOException
	{
		eCentFile = new File( FILE_LOCATION );
		eCents = new HashMap<Integer,String>();
		loadWallet();
	}
	
	// Add new eCent
	public boolean add(ECent ec, boolean sync) {
		if(eCents.containsKey(ec.seqNo)) // If already exists, throw exception
			throw new IllegalArgumentException("eCent already exists your fucked in the head cunt: "+ec.seqNo);
		else {
			eCents.put(ec.seqNo, ec.hash);
		}
		if(sync)
			this.saveWallet();
		return true;
	}
	public boolean add(ECent ec) {
		return this.add(ec,true);
	}
	
	// Add an array of eCents from string[]
	public void add(String[] eCentArray) {
		for (String eCent : eCentArray) {
			ECent newECent = new ECent(eCent);
			this.add(newECent,false);
		}
		
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
			BufferedWriter bw= new BufferedWriter(fw);
			
			bw.write(this.dumpECentsToString());
			bw.close();
			
			return true;
			
		} catch (IOException error) {
			System.out.println("Could not syncronise eCent Wallet in non-volatile memory!");
			return false;
		}
	}
	
	private String dumpECentsToString() {
		StringBuilder myString = new StringBuilder();
		for (Integer seqNo : eCents.keySet()) {
			myString.append(seqNo + ":" + eCents.get(seqNo) + "\n");
		}
		
		return myString.toString();
	}
	
	public String removeECent(){
		for(Integer seqNo : eCents.keySet()) {
			String hash = eCents.get(seqNo);
			eCents.remove(seqNo); // delete from wallet
			saveWallet(); // save wallet
			
			return seqNo + ":" + hash;
		}
		return null; //returns null if no eCents
	}

}
