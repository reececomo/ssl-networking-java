import java.io.*;

import java.security.*;
import java.util.*;
import javax.net.*;
import javax.net.ssl.*;
import java.net.InetAddress;
import java.net.UnknownHostException;

import lib.*;

/**
 * Analyst Class for analysing data
 * @author Jesse Fletcher, Caleb Fetzer, Reece Notargiacomo, Alexander Popoff-Asotoff
 * @version 5.9.15
 */

public class Analyst extends DemoMode {
	
	private static int dirPort = 9998;
	private static int bankPort = 9999;
	private static String directorIPAddress = "localhost";
	private static String bankIPAddress = "localhost";
	
	private final static String INVALID_DATA_MSG_RESPONSE = "INVALID\n";
	
	// for accessing parts of a message in an array
	private final static int DATA = 0;
	private final static int ECENT = 1;

	private SSLServerSocket sslserversocket = null;
	private int localPort;
	private boolean isRunning = true;
	
	private String outMsg;
	private String inMsg;

	public static void main(String[] args) throws IOException {
		// If IP Addresses given
		if( args.length == 2 ) {
			String[] bankFullAddress = args[0].split(":");
			String[] dirFullAddress = args[1].split(":");
			
			// Grab the first part
			bankIPAddress = bankFullAddress[0];
			directorIPAddress = dirFullAddress[0];
			
			// Test if ports ALSO given
			if (bankFullAddress.length == 2)
				bankPort = Integer.parseInt(bankFullAddress[1]);
			if (dirFullAddress.length == 2)
				dirPort = Integer.parseInt(dirFullAddress[1]);
		}

		// Run analyst
		new Analyst();
	}

	public Analyst() throws IOException {
		// Declare the node type for Demo mode
		set_type("ANALYST");
		
		// Declare SSL Certificate for both server and client connections
		SSLHandler.declareDualCert("SSL_Certificate","cits3002");

		ANNOUNCE("Connecting to Director");
		
		while(this.isRunning) {
			
			// If the socket doesn't deploy then
			//  retry the connection
			if(deployLocalSocket())
				this.run();
			else
				ALERT_WITH_DELAY("Retrying connection...");
		}
		
	}
	
	private boolean deployLocalSocket() {
		try {
			// Set up local Server
			SSLServerSocketFactory factory = (SSLServerSocketFactory)SSLServerSocketFactory.getDefault();
			sslserversocket = (SSLServerSocket) factory.createServerSocket(0);	// get any avail port (default 0)
			this.localPort = sslserversocket.getLocalPort();
			
			// Connect to Director
			return registerWithDirector();

		} catch (IOException err) {
			
			// Error setting up local socket
         		System.err.println("Error listening for a connection.");
			err.printStackTrace();
			return false;
		}
	}

	private void run() throws IOException {
		
		while (this.isRunning) {
			SSLSocket sslSocket = null;
			String[] message;
			
			try {
				// Recieve a new connection
				ANNOUNCE("Idle (Awaiting request from Director)");
				sslSocket = (SSLSocket)sslserversocket.accept();
				ANNOUNCE("Receiving request");
				
			} catch (IOException err) {
				System.err.println(err.getMessage());
			}

			// IO Buffers
			InputStream is = sslSocket.getInputStream();
			BufferedReader reader = new BufferedReader( new InputStreamReader(is) );
			OutputStreamWriter writer = new OutputStreamWriter( sslSocket.getOutputStream() );
			
       		// Accept input
			this.inMsg = reader.readLine();
			
			if ((message = decryptMessage(this.inMsg)) != null)
			{
				ALERT("Receiving payment");
				
				if ( depositMoney( message[ ECENT ] ))
					outMsg = analyseData("PARTY", message[ DATA ] );
				else
					outMsg = INVALID_DATA_MSG_RESPONSE; // eCent is invalid
				
				writer.write(outMsg+"\n");
				writer.flush();

			} else
				System.err.println("Could not decrypt message!");
		}
	}
	
	private String[] decryptMessage(String encryptedMsg) {
		// perform msg decryption here
		// remember to return string ARRAY
		return encryptedMsg.split(";");
	}
	
	private String analyseData(String analysisType, String rawdata) {
		// Example for data analysis
		// Analyse data here...
		if ( analysisType.equals("PARTY") )
			// if allowing for multiple analysis types
			return "WeLikeToParty!";
		else
			return "SomeGiberishDataResult!";
			
	}

	// Send data type to Director
	private boolean registerWithDirector() throws IOException{
		try{
			// set up socket to Dir
			SSLSocketFactory sslsf = (SSLSocketFactory)SSLSocketFactory.getDefault();
			SSLSocket sslsocket = (SSLSocket)sslsf.createSocket(directorIPAddress, dirPort);

			// prepare output stream (strings -> bytes)
			OutputStream outputstream = sslsocket.getOutputStream();
            		OutputStreamWriter outputstreamwriter = new OutputStreamWriter(outputstream); 
	
			ANNOUNCE("Registering availability with Director");

			// Analyst INIT message = [ INITFLAG  :  DATA TYPE  ;  ADDRESS  ;  PORT ]	address/port analyst is listening on
			outMsg = MessageFlag.A_INIT + ":" + "DATA" + ";" + getIPAddress() + ";" + Integer.toString(localPort) + "\n";

			outputstreamwriter.write( outMsg );
			outputstreamwriter.flush();

			sslsocket.close();
			
			return true;

		}catch (IOException e) {
			System.err.println("Could not connect to Director: " + e);
			return false;
		}
	}

	// deposit eCent in bank
	private boolean depositMoney(String eCent) throws IOException {
		try{
			ALERT("Contacting Bank");
			
			// SSL Socket
			SSLSocketFactory sslsf = (SSLSocketFactory)SSLSocketFactory.getDefault();
			SSLSocket sslSocket = (SSLSocket)sslsf.createSocket(bankIPAddress, bankPort); 

			// Create input buffer
			InputStream inputstream = sslSocket.getInputStream();
			InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
			BufferedReader bufferedreader = new BufferedReader(inputstreamreader);

			// Create output stream
			OutputStream outputStream = sslSocket.getOutputStream();
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream); 	

			ALERT("Sending eCent deposit...");
		
			// Send a bank deposit with the eCent
			outputStreamWriter.write(MessageFlag.BANK_DEP + ":" + eCent + "\n"); // send eCent to bank
			outputStreamWriter.flush();
			
			ALERT("(Awaiting confirmation)");
			inMsg = bufferedreader.readLine(); // receive confirmation

			ALERT("Response: " + inMsg);

			// If valid, return true
			return inMsg.equals("VALID");

		}catch (IOException e) {
			System.err.println("Could not achieve IO connection");
			System.exit(1);
		}
		return false;
	}

/** TO DO	
	// decrypt the given ecent
	// and return it to be deposited
	private decryptedEcent decrypt(Ecent eCent) {
		// todo
	}
		// perform the data analysis
	private void performAnalysis(Type dataToBeAnalysed) {

	}
**/
}
