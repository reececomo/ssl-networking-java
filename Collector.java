import java.io.*;
import java.util.Random;

import javax.net.*;
import javax.net.ssl.*;


public class Collector
{
	private static final int dirPort = 10000;
	private static final int bankPort = 9999;

	private static final String BANK_REQ = "REQ";		// bank withdrawl request flag
	private static final String BANK_WIT = "WITH";		// bank withdrawl accept flag (bank sends this with money)
	private static final String DIR_INIT = "INIT";		// director init flag
	private static final String EXAM_REQ = "DOIT";		// analysis request flag

	private static String outPacket;
	private static String inPacket;

	private static boolean ONLINE = false;			// boolean for determining if analyst availiable 
	private static File ecentFile;					// file for holding ecents
	
	
	/**
	 * Collector
	 */
	public static void main(String[] args) throws IOException{

		System.setProperty("javax.net.ssl.trustStore", "cits3002_01Keystore");
    		System.setProperty("javax.net.ssl.trustStorePassword", "cits3002");
		
		// set up packet in the form FLAG;MSG (ie REQ;AMOUNT)
		outPacket = BANK_REQ + ";10000\n";

		buyMoney();

		// set up packet in the form FLAG;MSG (ie REQ;DETAILS)
		outPacket = DIR_INIT + ";DATA\n";


		ONLINE = initDir();
		
		//initialize ecent file to current directory
		ecentFile = new File("ecents.txt");
		if (ecentFile.exists()){
			System.out.println("File already exists");
		}
		else{
			System.out.println("File doesn't exist.\nCreating eCent file...");
			FileWriter fw = new FileWriter(ecentFile.getAbsoluteFile());
			BufferedWriter bw= new BufferedWriter(fw);
			bw.write("");
			bw.close();
		}

		// collects an array of randomly generated ints for basic analysis (perhaps an average)
		int[] data = collect();
	}

	private static void buyMoney() throws IOException{
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

			// write packet to outputstreamwriter (Note: bufferedwriter isn't needed since we don't need to buffer system input)
			outputstreamwriter.write(outPacket);
			outputstreamwriter.flush();
			
			inPacket = bufferedreader.readLine();
			
			
			System.out.println(inPacket);			// print money recieved (this would IO pipe into file)

		}catch (IOException e)
		{
			System.err.println("Could not achieve IO connection");
			System.exit(1);
		}
	}
	
	// Returns TRUE iff director can handle data analysis (has analyst(s) availiable)
	private static boolean initDir() throws IOException{
		try{
			// set up Socket to bank
			SSLSocketFactory sslsf = (SSLSocketFactory)SSLSocketFactory.getDefault();
			SSLSocket sslsocket = (SSLSocket)sslsf.createSocket("localhost", dirPort);

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

			System.out.println("Sending Director Initialization Request..");

			// write packet to outputstreamwriter (Note: bufferedwriter isn't needed since we don't need to buffer system input)
			outputstreamwriter.write(outPacket);
			outputstreamwriter.flush();

			inPacket = bufferedreader.readLine(); 
			System.out.println(inPacket);			// print money recieved (this would IO pipe into file)

			if(inPacket.substring(4,inPacket.length()).equals("TRUE"))
				return true;
			else return false;

		}catch (IOException e)
		{
			System.err.println("Could not achieve IO connection");
			System.exit(1);
		}
		

		return false;
	}
	private static int[] collect(){
		int[] array= new int[10];
		Random rand = new Random();
		for (int i=0; i<10; i++){
			array[i]=rand.nextInt();
		}
		return array;
	}
}
