import java.io.*;

import javax.net.ssl.*;

import java.security.*;
import java.util.*;

import lib.*;

/**
 * Bank Class
 * For handling Ecent generation, checking and depositing
 * @author Jesse Fletcher, Caleb Fetzer, Reece Notargiacomo, Alexander Popoff-Asotoff
 * @version 5.9.15
 */

public class Bank extends DemoMode {
    
	private static int bankPort = 9999;
	 
	private static int sequence = 0;			
	private static int validationKey; 
	
	private static final String BANK_REQ = "REQ";		// withdrawl flag
	private static final String BANK_DEP = "DEP";		// deposit flag

	private static SSLServerSocket sslserversocket = null;

	private static ECentWallet bankStore;
	private final static String ECENTWALLET_FILE = "bank.wallet";

	/**
	 * Bank 
	 */

	public static void main(String[] args) throws IOException {
		// Option to give the port as an argument
		if (args.length == 1)
			try{ bankPort = Integer.valueOf(args[0]); }
			catch (NumberFormatException er) { bankPort = 9999; }
		
		
		new Bank();
	}
	
	public Bank() throws IOException {
		set_type("BANK");
		SSLHandler.declareServerCert("SSL_Certificate","cits3002");
		
		bankStore = new ECentWallet( ECENTWALLET_FILE );
    	
		//Generates validation key for hashing
		SecureRandom random = new SecureRandom();
		validationKey = random.nextInt(32);
		
		ANNOUNCE("Starting bank server");

		if(this.startServer()) {

			ANNOUNCE("Bank started on " + getIPAddress() + ":" + bankPort);
			
			while(true){
				SSLSocket sslsocket = null;
				try {
					sslsocket = (SSLSocket)sslserversocket.accept();
					
					ALERT("Receiving new request!");

				}catch (IOException e){ System.out.println("Error connecting client"); }

				new Thread( new bankConnection(sslsocket) ).start();	// start new thread
			}
		}
	}
	
	private boolean startServer() {
		try {
			// Use the SSLSSFactory to create a SSLServerSocket to create a SSLSocket
			SSLServerSocketFactory sslserversocketfactory = (SSLServerSocketFactory)SSLServerSocketFactory.getDefault();
			sslserversocket = (SSLServerSocket)sslserversocketfactory.createServerSocket(bankPort);
			return true;
		} catch (IOException e) {
			ALERT("Could not create server on port " + bankPort);
		}
		return false;
	}

	private class bankConnection implements Runnable {

		protected SSLSocket sslsocket = null;

		public bankConnection(SSLSocket socket){
			this.sslsocket = socket;
		}

		public void run() {
			try {
				InputStream inputstream = sslsocket.getInputStream();
				InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
				BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
				OutputStream outputstream = sslsocket.getOutputStream();
            	OutputStreamWriter outputstreamwriter = new OutputStreamWriter(outputstream);
            	
		
				String message = bufferedreader.readLine();			// get message from client
				Message msg = new Message(message);
				String msg_flag = msg.getFlag();

				if(msg_flag.equals(MessageFlag.BANK_WIT)){			// WITHDRAWL FLAG HANDLING

					ALERT("Collector connected  -->  Withdrawing money");
					
					int amount = Integer.parseInt(msg.data);
					
					ALERT("Generating " + amount + " eCents!");
	
					for(int i=0; i<amount; i++)
						outputstreamwriter.write( generateEcent() + "\n" );
					
					outputstreamwriter.flush();

					ALERT("Money sent");

				}else if(msg_flag.equals(MessageFlag.BANK_DEP)){		// DEPOSIT FLAG HANDLING

					ALERT("Analyst connected  -->  Depositing money");

					//Check if eCent is in valid eCent set
					if( bankStore.contains(msg.data) ) {
						
						ALERT("Depositing valid eCent");
						ALERT("Sending acknowledgement to Analyst!");
						outputstreamwriter.write("VALID\n");	
						bankStore.remove(msg.data);
						
					} else {
						
						ALERT("Rejecting invalid eCent");
						outputstreamwriter.write("INVALID\n");
					}

					outputstreamwriter.flush();
				
				}

				sslsocket.close();
				
				ALERT("Request finished!");

			} catch (IOException e) { System.out.println("Error listening on port: " + bankPort); }
		}
	}

	
	private static String generateEcent(){

		return getSHA256Hash(Integer.toString(sequence++));
	}

	private static String getSHA256Hash(String passwordToHash){

		String generatedPassword = null;
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			String salt = getSalt();
			md.update(salt.getBytes());
			byte[] bytes = md.digest(passwordToHash.getBytes());
			StringBuilder sb = new StringBuilder();
			for(int i=0; i< bytes.length ;i++){
				sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
			}
			generatedPassword = sb.toString();
		}
		catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}
		bankStore.add(generatedPassword);		// add ecent to valid set
		return generatedPassword;			// return ecent
	}
        
	private static String getSalt() throws NoSuchAlgorithmException
	{
		SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
		byte[] salt = new byte[16];
		sr.nextBytes(salt);
		return salt.toString();
	}

}
