import java.io.*;
import lib.*;
import java.util.Random;

import javax.net.*;
import javax.net.ssl.*;

/**
 * Collector Class
 * @author Jesse Fletcher, Caleb Fetzer, Reece Notargiacomo, Alexander Popoff-Asotoff
 * @version 5.9.15
 */

public class Collector
{
	private final static int dirPort = 9998;
	private final static int bankPort = 9999;

	private final static String ECENTWALLET_FILE = "ecents.wallet";

	private String outMessage;
	private String inMessage;

	private static String directorIPAddress = "localhost";
	private static String bankIPAddress = "localhost";

	private ECentWallet eCentWallet; // file for holding ecents

	private SSLSocketFactory dirsf;
	private SSLSocket dirsslsocket;
	private OutputStreamWriter directorWriter;
	private BufferedReader directorReader;
	private InputStream dirinputstream;
	private InputStreamReader dirinputstreamreader;
	private OutputStream diroutputstream;
	
	
	/**
	 * Collector
	 */
	public static void main(String[] args) throws IOException {
		// If IP Addresses given
		if( args.length == 2 ) {
			directorIPAddress = args[0];
			bankIPAddress = args[1];
		}
		
		// Start collector
		Collector collector = new Collector();
	}
	
	public Collector() throws IOException {
		SSLHandler.declareClientCert("cits3002_01Keystore","cits3002");
		
		// Initiate eCentWallet
		this.eCentWallet = new ECentWallet( ECENTWALLET_FILE );
		if(eCentWallet.isEmpty()){ buyMoney(); }
		this.eCentWallet.displayBalance();
		
		// set up message in the form FLAG;MSG (ie REQ;AMOUNT)
		outMessage = MessageFlag.C_INIT + ":DATA\n";

		if(connectToDirector())
			sendDirectorData();
	}


	private void buyMoney() throws IOException{
		try{
			// set up Socket to bank
			SSLSocketFactory sslsf = (SSLSocketFactory)SSLSocketFactory.getDefault();
			SSLSocket sslsocket = (SSLSocket)sslsf.createSocket(bankIPAddress, bankPort);

			// (FOR RECIEVING MONEY) -
			// Create an input stream (FOR RECIEVING MONEY) (bytes -> chars)
			InputStream inputstream = sslsocket.getInputStream();
			InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
			// Create a buffered reader (chars -> strings) (BUFFEREDREADER NEEDS NEWLINE ENDED STRINGS TO WORK)
			BufferedReader bufferedreader = new BufferedReader(inputstreamreader);

			// (FOR SENDING REQUEST) -
			// prepare output stream (strings -> bytes)
			OutputStream outputstream = sslsocket.getOutputStream();
            OutputStreamWriter outputstreamwriter = new OutputStreamWriter(outputstream); 

			System.out.println("Sending Money Withdrawl Request..");

			outMessage = MessageFlag.BANK_WIT + ":100\n";

			outputstreamwriter.write(outMessage);
			outputstreamwriter.flush();
			
			String[] eCentBuffer = new String[100];

			int index = 0;
			while((inMessage = bufferedreader.readLine()) != null){
			//	System.out.println(index + " = " + inMessage);	
				eCentBuffer[index] = inMessage;
				index++;
			}

			eCentWallet.add(eCentBuffer);

		}catch (IOException e)
		{
			System.err.println("Could not connect to Bank");
			System.exit(1);
		}
	}
	
	// Returns TRUE iff director can handle data analysis (has analyst(s) availiable)
	private boolean connectToDirector() throws IOException{
		try{
			dirsf = (SSLSocketFactory)SSLSocketFactory.getDefault();
			dirsslsocket = (SSLSocket) dirsf.createSocket(directorIPAddress, dirPort);

			dirinputstream = dirsslsocket.getInputStream();
			dirinputstreamreader = new InputStreamReader(dirinputstream);
			diroutputstream = dirsslsocket.getOutputStream();

			directorReader = new BufferedReader(dirinputstreamreader);
            directorWriter = new OutputStreamWriter(diroutputstream); 

			System.out.println("Sending Director Initialization Request..");

			// write message to outputstreamwriter (Note: bufferedwriter isn't needed since we don't need to buffer system input)
			directorWriter.write(outMessage + "\n");
			directorWriter.flush();

			inMessage = directorReader.readLine(); 			

			return inMessage.equals("TRUE");

		} catch (IOException e) {
			System.err.println("Could not connect to Director");
		}
		return false;
	}

	private void sendDirectorData() throws IOException {
		
		System.out.println("Initiated with Director!");
		String temporary_eCent = eCentWallet.remove();

		try{
			dirsf = (SSLSocketFactory)SSLSocketFactory.getDefault();
			dirsslsocket = (SSLSocket) dirsf.createSocket(directorIPAddress, dirPort);

			dirinputstream = dirsslsocket.getInputStream();
			dirinputstreamreader = new InputStreamReader(dirinputstream);
			diroutputstream = dirsslsocket.getOutputStream();

			directorReader = new BufferedReader(dirinputstreamreader);
            directorWriter = new OutputStreamWriter(diroutputstream); 
			// send data message = [ FLAG  :  DATA TYPE  ;  DATA  ;  ECENT  ]
			outMessage = MessageFlag.EXAM_REQ + ":" + "DATA" + ";blahblahblah;" + temporary_eCent + "\n";

			System.out.print("The message I'm sending for analysis is:\n" + outMessage);

			directorWriter.write(outMessage); // send request to director (FLAG:TYPE;DATA;ECENT)
			directorWriter.flush();

			inMessage = directorReader.readLine(); // read result returned by director
			System.out.println("RESULT I PAID FOR: " + inMessage);

			dirsslsocket.close();
		}
		catch (IOException e){
			System.err.println("Error sending data to Director" + e);
			// Put eCent back in wallet if fucked up
			this.eCentWallet.add( temporary_eCent );
		}

	}

	private int[] collect(){
		int[] array= new int[10];
		Random rand = new Random();
		for (int i=0; i<10; i++){
			array[i]=rand.nextInt();
		}
		return array;
	}
}
