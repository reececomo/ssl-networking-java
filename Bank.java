import java.io.*;
import javax.net.ssl.*;
import java.security.SecureRandom;

public class Bank
{
    
	private static final int bankPort = 9999;
	 
	private int sequence = 0; //this should be changed later if we want to make the sequence number permanent(stored in file)
	private static int validationKey; 
	
	private static final String BANK_REQ = "REQ";		// withdrawl flag
	private static final String BANK_DEP = "DEP";		// deposit flag

	private static SSLServerSocket sslserversocket = null;

	/**
	 * Bank 
	 */

	public static void main(String[] args) throws IOException{

		System.setProperty("javax.net.ssl.keyStore", "cits3002_01Keystore");
    		System.setProperty("javax.net.ssl.keyStorePassword", "cits3002");
    	
    	//Generates validation key for hashing
    	SecureRandom random = new SecureRandom();
    	validationKey= random.nextInt(32);

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

					String amount = packet.substring(4, packet.length()) + "\n";

					// Ecent encryption
					int am = Integer.parseInt(amount);
					int[] centArray= new int[am];
					centArray=generateEcent(am);
					
					outputstreamwriter.write(amount);
					outputstreamwriter.flush();
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
		
		//needs to be fully implemented
		private int[] generateEcent(int amount){
			int[] centArray = new int[amount];
			for (int i=0; i<amount; i++){
				
			}
			return centArray;
		}
	}
