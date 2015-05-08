import java.io.*;
import javax.net.ssl.*;
import java.security.*;
import java.util.*;

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

		System.setProperty("javax.net.ssl.keyStore", "cits3002_01Keystore");
    		System.setProperty("javax.net.ssl.keyStorePassword", "cits3002");
    	
    		//Generates validation key for hashing
    		SecureRandom random = new SecureRandom();
    		validationKey= random.nextInt(32);

		bankStore = new HashSet<String>();

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
				System.out.println("Client Connected");

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

				// Create an input stream (bytes -> chars)
				InputStream inputstream = sslsocket.getInputStream();
				InputStreamReader inputstreamreader = new InputStreamReader(inputstream);

				// Create a buffered reader (chars -> strings) (NOTE: BufferedReader needs need a NEWLINE ended message to readLine() properly)
				BufferedReader bufferedreader = new BufferedReader(inputstreamreader);

				// Create output stream
				OutputStream outputstream = sslsocket.getOutputStream();
            			OutputStreamWriter outputstreamwriter = new OutputStreamWriter(outputstream);
		
				System.out.println("Thread Started");
		
				String packet = bufferedreader.readLine();

				String flag = packet.substring(0,3);

				if(flag.equals("REQ")){

					String amount = packet.substring(4, packet.length());

					// Ecent encryption
					int am = Integer.parseInt(amount);
	
					for(int i=0; i<am; i++){
						outputstreamwriter.write(generateEcent());
						outputstreamwriter.flush();
					}

					System.out.println("Money Sent");

				}else if(flag.equals("DEP")){

					// Handle deposit/Ecent checking
				}

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
		bankStore.add(generatedPassword);
		return generatedPassword;
	}
        
	//Add salt
	private static String getSalt() throws NoSuchAlgorithmException
	{
		SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
		byte[] salt = new byte[16];
		sr.nextBytes(salt);
		return salt.toString();
	}

}
