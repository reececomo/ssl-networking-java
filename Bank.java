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

public class Bank
{
    
	private static final int bankPort = 9999;
	 
	private static int sequence = 0;			
	private static int validationKey; 
	
	private static final String BANK_REQ = "REQ";		// withdrawl flag
	private static final String BANK_DEP = "DEP";		// deposit flag

	private static SSLServerSocket sslserversocket = null;

	private static HashSet<String> bankStore;

	/**
	 * Bank 
	 */

	public static void main(String[] args) throws IOException{

		SSLHandler.declareServerCert("cits3002_01Keystore","cits3002");
    	
    		//Generates validation key for hashing
    		SecureRandom random = new SecureRandom();
    		validationKey= random.nextInt(32);

		bankStore = new HashSet<String>();		// Set of all valid ecent hashes

		try {
			// Use the SSLSSFactory to create a SSLServerSocket to create a SSLSocket
			SSLServerSocketFactory sslserversocketfactory = (SSLServerSocketFactory)SSLServerSocketFactory.getDefault();
			sslserversocket = (SSLServerSocket)sslserversocketfactory.createServerSocket(9999);
			System.out.println("Bank Server Started");

		} catch (IOException e)
        	{
            		System.out.println("Error listening on port 9999 or listening for a connection");
			System.out.println(e.getMessage());
        	}

		while(true){
			SSLSocket sslsocket = null;
			try {
				sslsocket = (SSLSocket)sslserversocket.accept();

			}catch (IOException e){
				System.out.println("Error connecting to client");
				System.out.println(e.getMessage());
			}

			new Thread( new bankConnection(sslsocket) ).start();	// start new thread
		}
	}

	private static class bankConnection implements Runnable {

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
		
				String packet = bufferedreader.readLine();			// get message from client
				Message msg = new Message(packet);				

				if(msg.getFlag().equals(MessageFlag.BANK_WIT)){			// WITHDRAWL FLAG HANDLING

					System.out.println("Collector connected\nWithdrawing money..");
					String amount = msg.data;

					// Ecent encryption
					int am = Integer.parseInt(amount);
	
					String ecent;
					for(int i=0; i<am; i++){
						ecent = generateEcent() + "\n";
						outputstreamwriter.write(ecent);
						outputstreamwriter.flush();
					}
					System.out.println("Money Sent");

				}else if(msg.getFlag().equals(MessageFlag.BANK_DEP)){		// DEPOSIT FLAG HANDLING
					
					System.out.println("Analyst connected\nDepositing money..");

					//Check if Ecent is in valid Ecent set
					if(bankStore.contains(msg.data)) outputstreamwriter.write("TRUE\n");	
					else outputstreamwriter.write("FALSE\n");

					outputstreamwriter.flush();
					System.out.println("Money Deposited");
				
				}

				sslsocket.close();

			} catch (IOException e)
        		{
           	 		System.out.println("Error listening on port 9999 or listening for a connection");
				System.out.println(e.getMessage());
        		}
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
