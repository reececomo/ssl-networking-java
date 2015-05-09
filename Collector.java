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
	private final int dirPort = 9998;
	private final int bankPort = 9999;

	private String outPacket;
	private String inPacket;

	private boolean ONLINE = false;			// boolean for determining if analyst availiable 
	private ECentWallet eCentWallet;					// file for holding ecents
	
	
	/**
	 * Collector
	 */
	public static void main(String[] args) throws IOException {
		Collector myCol = new Collector();
	}
	
	public Collector() throws IOException {
		SSLHandler.declareClientCert("cits3002_01Keystore","cits3002");
		
		// Initiate eCentWallet
		eCentWallet = new ECentWallet();
		if (eCentWallet.isEmpty())
			buyMoney();
		
		eCentWallet.displayBalance();
		
		// set up packet in the form FLAG;MSG (ie REQ;AMOUNT)
		outPacket = MessageFlag.BANK_WIT + ":100\n";
		outPacket = MessageFlag.C_INIT + ":DATA\n";

		ONLINE = initDir();
		
		if(ONLINE)
			sendData();
	}


	private void buyMoney() throws IOException{
		try{
			// set up Socket to bank
			SSLSocketFactory sslsf = (SSLSocketFactory)SSLSocketFactory.getDefault();
			SSLSocket sslsocket = (SSLSocket)sslsf.createSocket("localhost", bankPort);

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

			outputstreamwriter.write(outPacket);
			outputstreamwriter.flush();
			
			String[] eCentBuffer = new String[100];

			int index = 0;
			while((inPacket = bufferedreader.readLine()) != null){
			//	System.out.println(index + " = " + inPacket);	
				eCentBuffer[index] = inPacket;
				index++;
			}

			eCentWallet.add(eCentBuffer);

		}catch (IOException e)
		{
			System.err.println("Could not achieve IO connection");
			System.exit(1);
		}
	}
	
	// Returns TRUE iff director can handle data analysis (has analyst(s) availiable)
	private boolean initDir() throws IOException{
		try{
			SSLSocketFactory sslsf = (SSLSocketFactory)SSLSocketFactory.getDefault();
			SSLSocket sslsocket = (SSLSocket)sslsf.createSocket("localhost", dirPort);

			InputStream inputstream = sslsocket.getInputStream();
			InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
			BufferedReader bufferedreader = new BufferedReader(inputstreamreader);

			OutputStream outputstream = sslsocket.getOutputStream();
            		OutputStreamWriter outputstreamwriter = new OutputStreamWriter(outputstream); 

			System.out.println("Sending Director Initialization Request..");

			// write packet to outputstreamwriter (Note: bufferedwriter isn't needed since we don't need to buffer system input)
			outputstreamwriter.write(outPacket);
			outputstreamwriter.flush();

			inPacket = bufferedreader.readLine(); 			

			if(inPacket.equals("TRUE"))
				return true;
			else return false;

		}catch (IOException e)
		{
			System.err.println("Could not achieve IO connection");
			System.exit(1);
		}
		return false;
	}

	private void sendData() throws IOException{
		try{
			SSLSocketFactory sslsf = (SSLSocketFactory)SSLSocketFactory.getDefault();
			SSLSocket sslsocket = (SSLSocket)sslsf.createSocket("localhost", dirPort);

			InputStream inputstream = sslsocket.getInputStream();
			InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
			BufferedReader bufferedreader = new BufferedReader(inputstreamreader);

			OutputStream outputstream = sslsocket.getOutputStream();
            		OutputStreamWriter outputstreamwriter = new OutputStreamWriter(outputstream); 

			// send data packet = [ FLAG  :  DATA TYPE  ;  DATA  ;  ECENT  ]
			outPacket = MessageFlag.EXAM_REQ + ":" + "DATA" + ";blahblahblah;" + eCentWallet.remove() + "\n";

			System.out.print("THE PACKET I'M SENDING FOR ANALYSIS IS:\n" + outPacket);

			outputstreamwriter.write(outPacket);			// send request to director (FLAG:TYPE;DATA;ECENT)
			outputstreamwriter.flush();

			inPacket = bufferedreader.readLine();			// read result returned by director
			System.out.println("RESULT I PAID FOR: " + inPacket);

			sslsocket.close();
		}
		catch (IOException e){
			System.err.println("Could not achieve IO connection");
			System.exit(1);
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
