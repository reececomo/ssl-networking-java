import java.io.*;

import lib.*;

import java.security.*;
import java.util.*;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.net.*;
import javax.net.ssl.*;

/**
 * Analyst Class for analysing data
 * @author Jesse Fletcher, Caleb Fetzer, Reece Notargiacomo, Alexander Popoff-Asotoff
 * @version 5.9.15
 */
public class Analyst {
	
	private final static int dirPort = 9998;
	private final static int bankPort = 9999;
	private final static int DEFAULT_PAUSE_LENGTH = 5000; // 5000 milliseconds
	private final static String INVALID_DATA_MSG_RESPONSE = "FALSEINVALID\n";
	
	// for accessing parts of a message in an array
	private final static int DATA = 0;
	private final static int ECENT = 1;
	
	private static String directorIPAddress = "localhost";
	private static String bankIPAddress = "localhost";

	private SSLServerSocket sslserversocket = null;
	private int localPort;
	private boolean isRunning = true;
	
	private String outMsg;
	private String inMsg;

	public static void main(String[] args) throws IOException {
		// If IP Addresses given
		if( args.length == 2 ) {
			directorIPAddress = args[0];
			bankIPAddress = args[1];
		}

		// Run analyst
		Analyst analyst = new Analyst();
	}

	public Analyst() throws IOException {
		// Declare SSL Certificate for both server and client connections
		SSLHandler.declareDualCert("cits3002_01Keystore","cits3002");
		
		while(this.isRunning) {
			// If the socket doesn't deploy then
			//  retry the connection
			if(deployLocalSocket())
				this.run();
			else
				this.delayWithMessage("Retrying connection...");
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
			// Accept a new connection
			SSLSocket sslSocket = null;
			try {
				System.out.println("Awaiting connection from Director...");
				sslSocket = (SSLSocket)sslserversocket.accept();
				System.out.println("Director connected. ");
			} catch (IOException err) {
				System.err.println(err.getMessage());
			}

			// Prepare the io streams
			InputStream is = sslSocket.getInputStream();
			BufferedReader reader = new BufferedReader( new InputStreamReader(is) );
			OutputStreamWriter writer = new OutputStreamWriter( sslSocket.getOutputStream() );
       	
       			// Read a line
			this.inMsg = reader.readLine();
			String[] message;
			if ((message = decryptMessage(this.inMsg)) != null) {
				
				this.delayWithMessage("Preparing to deposit.");
	
				if (this.depositMoney( message[ ECENT ] ))
					outMsg = this.analyseData("PARTY", message[ DATA ] );
				else
					outMsg = INVALID_DATA_MSG_RESPONSE; // eCent is invalid
	
				this.delayWithMessage("Deposited. Returning result to Director:" + outMsg);
				writer.write(outMsg + "\n");
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
	
			System.out.println("Sending Director Initialization..");

			// Analyst INIT message = [ INITFLAG  :  DATA TYPE  ;  ADDRESS  ;  PORT ]	address/port analyst is listening on
			outMsg = MessageFlag.A_INIT + ":" + "DATA" + ";" + getIPAddress() + ";" + Integer.toString(localPort) + "\n";

			outputstreamwriter.write( outMsg );
			outputstreamwriter.flush();

			sslsocket.close();
			
			return true;

		}catch (IOException e) {
			System.err.println("Could not connect to Director");
			e.printStackTrace();
			return false;
		}
	}

	public String getIPAddress() {
		try {
			//Get IP Address
			return InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			return "UnknownHost";
		}
	}
	
	private void delayWithMessage(String message) {
		try {
			System.out.println(">> " + message);
			Thread.sleep( DEFAULT_PAUSE_LENGTH );
			
		} catch (Exception err) { err.printStackTrace(); }
	}


	// deposit eCent in bank
	private boolean depositMoney(String eCent) throws IOException {
		try{
			// set up Socket to bank
			SSLSocketFactory sslsf = (SSLSocketFactory)SSLSocketFactory.getDefault();
			SSLSocket sslSocket = (SSLSocket)sslsf.createSocket(bankIPAddress, bankPort); 

			InputStream inputstream = sslSocket.getInputStream();
			InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
			BufferedReader bufferedreader = new BufferedReader(inputstreamreader);

			System.out.println("Sending eCent deposit request...");

			OutputStream outputStream = sslSocket.getOutputStream();
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream); 	
		
			outMsg = MessageFlag.BANK_DEP + ":" + eCent + "\n";

			outputStreamWriter.write(outMsg);		// send ecent to bank
			outputStreamWriter.flush();

			inMsg = bufferedreader.readLine();		// recieve confirmation

			this.delayWithMessage("Message recieved: " + inMsg);

			return inMsg.equals("TRUE");

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
