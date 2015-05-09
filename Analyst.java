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

	private final int dirPort = 9998;
	private final int bankPort = 9999;
	private int myPort;
	private static SSLServerSocket sslserversocket = null;

    // A_INIT FLAG // of messageflag INIA

	private String outPacket;
	private String inPacket;

	// is the analyst available? director wants to know.

	public static void main(String[] args) throws IOException {
		Analyst myAnal = new Analyst();
	}

	public Analyst() throws IOException {

		SSLHandler.declareClientCert("cits3002_01Keystore","cits3002");
		SSLHandler.declareServerCert("cits3002_01Keystore","cits3002");
		
		getMoney();


	}

	// starting getting data (ecent included) from Director(s)
	private void getMoney() throws IOException {
		
		try {

			// Use the SSLSSFactory to create a SSLServerSocket to create a SSLSocket
			SSLServerSocketFactory sslserversocketfactory = (SSLServerSocketFactory)SSLServerSocketFactory.getDefault();
			sslserversocket = (SSLServerSocket)sslserversocketfactory.createServerSocket(0); 		// get any avail port (default 0)

			myPort = sslserversocket.getLocalPort();							// save this port for laterez

			initDir();			// init with Director once (after setting up server so you can send him your port :) )

		} catch (IOException e) {
         		System.out.println("Error listening on port 9997 or listening for a connection");
			System.out.println(e.getMessage());
	        }
		while(true){
			
			SSLSocket sslSocket = null;
			try {
				System.out.println("Waiting for data to analyse...");
				sslSocket = (SSLSocket)sslserversocket.accept();
				System.out.println("Director connected. ");

				

			} catch (IOException e) {
				System.out.println("Error connecting to director");
				System.out.println(e.getMessage());
			}

			InputStream inputstream = sslSocket.getInputStream();
			InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
			BufferedReader bufferedreader = new BufferedReader(inputstreamreader);		

			OutputStream outputstream = sslSocket.getOutputStream();
			OutputStreamWriter outputstreamwriter = new OutputStreamWriter(outputstream);
       	
			inPacket = bufferedreader.readLine();
			
			// String decryption = decryptPacket(inPacket);		//decrypt packet here

			String data = inPacket.split(";")[0];
			String eCent = inPacket.split(";")[1];

			if(depositMoney(eCent)){

				// ANALYSE DATA

				outPacket = "SOMEGIBERISHDATARESULTALLGOODBRU\n";	// RETURN RESULT
			}else{
				outPacket = "FALSEINVALID\n";		// ECENT NOT VALID
			}

			System.out.println("Result returned to Director = " + outPacket);
			outputstreamwriter.write(outPacket);
			outputstreamwriter.flush();
			
			// ANALYST IS DONE LOOP BACK AND LISTEN FOR SOMETHING ELSE

		}
        
	}

	// Send data type to Director
	private void initDir() throws IOException{
		try{
			// set up socket to Dir
			SSLSocketFactory sslsf = (SSLSocketFactory)SSLSocketFactory.getDefault();
			SSLSocket sslsocket = (SSLSocket)sslsf.createSocket("localhost", dirPort);

			// prepare output stream (strings -> bytes)
			OutputStream outputstream = sslsocket.getOutputStream();
            		OutputStreamWriter outputstreamwriter = new OutputStreamWriter(outputstream); 
	
			System.out.println("Sending Director Initialization..");

			// Analyst INIT packet = [ INITFLAG  :  DATA TYPE  ;  ADDRESS  ;  PORT ]	address/port analyst is listening on
			outPacket = MessageFlag.A_INIT + ":" + "DATA" + ";" + getIPAddress() + ";" + Integer.toString(myPort) + "\n";

			outputstreamwriter.write(outPacket);
			outputstreamwriter.flush();

			sslsocket.close();

		}catch (IOException e)
		{
			System.err.println("Could not achieve IO connection");
			System.exit(1);
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


	// deposit eCent in bank
	private boolean depositMoney(String eCent) throws IOException {
		try{
			// set up Socket to bank
			SSLSocketFactory sslsf = (SSLSocketFactory)SSLSocketFactory.getDefault();
			SSLSocket sslSocket = (SSLSocket)sslsf.createSocket("localhost", bankPort); 

			InputStream inputstream = sslSocket.getInputStream();
			InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
			BufferedReader bufferedreader = new BufferedReader(inputstreamreader);

			System.out.println("Sending eCent deposit request...");

			OutputStream outputStream = sslSocket.getOutputStream();
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream); 	
		
			outPacket = MessageFlag.BANK_DEP + ":" + eCent + "\n";

			outputStreamWriter.write(outPacket);		// send ecent to bank
			outputStreamWriter.flush();

			inPacket = bufferedreader.readLine();		// recieve confirmation

			System.out.println(inPacket);

			if(inPacket.equals("TRUE"))
				return true; 	// successful deposit
			else return false;

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
