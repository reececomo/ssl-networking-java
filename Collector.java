import java.io.*;
import java.util.Random;
import javax.net.ssl.*;
import java.util.Arrays;
import lib.*;

/**
 * Collector Class
 * @author Jesse Fletcher, Caleb Fetzer, Reece Notargiacomo, Alexander Popoff-Asotoff
 * @version 5.9.15
 */

public class Collector extends Node {

	private static int dirPort = 9998;
	private static int bankPort = 9999;
	private static String directorIPAddress = "localhost";
	private static String bankIPAddress = "localhost";

	private final static String ECENTWALLET_FILE = "collector.wallet";

	private String outMessage;
	private String inMessage;

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
		
		// Start collector
		new Collector();
	}
	
	public Collector() throws IOException {
		set_type("COLLECTOR");
		SSLHandler.declareClientCert("SSL_Certificate","cits3002");
		
		// Initiate eCentWallet
		this.eCentWallet = new ECentWallet( ECENTWALLET_FILE );
		if (eCentWallet.isEmpty())
			buyMoney();
		ANNOUNCE(eCentWallet.displayBalance());
		
		// Prepare the Collector Initiate message
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

			ALERT("Sending Money Withdrawl Request..");

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

            ALERT("Connecting to Director..");

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
		
		ALERT("Connected! (Director)");
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
			outMessage = MessageFlag.EXAM_REQ + ":" + "DATA" + ";" + collect() + ";" + temporary_eCent + "\n";

			directorWriter.write(outMessage); // send request to director (FLAG:TYPE;DATA;ECENT)
			directorWriter.flush();
			
			ALERT("Request sent!");
			ALERT("...");

			inMessage = directorReader.readLine(); // read result returned by directorALERTALERT
			
			ALERT("Response recieved!");
			ALERT("RESULT: " + inMessage);

			dirsslsocket.close();
		}
		catch (IOException e){
			System.err.println("Error sending data to Director" + e);
			// Put eCent back in wallet if fucked up
			this.eCentWallet.add( temporary_eCent );
		}

	}

	private String collect(){
		int[] array= new int[10];
		Random rand = new Random();
		for (int i=0; i<10; i++){
			array[i]=rand.nextInt();
		}
		System.out.println(Arrays.toString(array));	
		return Arrays.toString(array);
	}
}
