// Kill me now
import java.io.*;
import lib.*;

import javax.net.*;
import javax.net.ssl;

/* The analysistsstissys class for analsaydasdjhaising the data
 * 
 */
public class Analyst {

	private final int dirPort = 10000;
	private final int bankPort = 9999;

    // A_INIT FLAG // of messageflag INIA

	private String outPacket;
	private String inPacket;

	// is the analyst available? director wants to know.

	public static void main(String[] args) throws IOException {
		Analyst myAnal = new Analyst();
	}

	public Analyst() throws IOException {

		SSLHandler.declareClientCert("cits3002_01Keystore","cits3002");
		
		// send a initililisation flag to director + registration
		outPacket = MessageFlag.INIA + ";DATA\n";
		// receive data analysis request flag from director
		// inPacket = MessageFlag.DOIT + ";DATA\n"; // eCent;DATA
		// send deposit request flag to bank
		outPacket = MessageFlag.DEPOSIT + eCent;
		// "deposit allowed/not allowed" flag from Bank;
		// inPacket = MessageFlag.ACK;
		// send data analysis flag to director;
		outPacket = MessageFlag.ANALYSE_SHIT;

		Message msg = new Message(packet);

		initDir();
		// sit here and listen for director
		getMoney();


	}

	// deposit eCents
	private boolean depositMoney(Ecent eCent) throws IOException {
		// set up Socket to bank
		SSLSocketFactory sslsf = (SSLSocketFactory)SSLSocketFactory.getDefault();
		SSLSocket sslSocket = (SSLSocket)sslsf.createSocket("localhost", bankPort); 

		// get bank confirmation
		InputStream inputstream = sslsocket.getInputStream();
		InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
		// Create a buffered reader (chars -> strings) (BUFFEREDREADER NEEDS NEWLINE ENDED STRINGS TO WORK)
		BufferedReader bufferedreader = new BufferedReader(inputstreamreader);

		System.out.println("Sending eCent deposit request...");
		// prepare output stream (strings -> bytes)
		// in order to send the ecents to bank 
		OutputStream outputStream = sslsocket.getOutputStream();
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream); 	
		
		outputStreamWriter.write(outPacket);
		outputStreamWriter.flush();

		if(inPacket.substring(4,inPacket.length()).equals("TRUE"))
			return true; // successful deposit
		else return false;
	
	}

	// starting getting data (ecent included) from Director(s)
	private void getMoney() throws IOException {
		
		try {
			// Use the SSLSSFactory to create a SSLServerSocket to create a SSLSocket
			SSLServerSocketFactory sslServerSocketFactory = (SSLServerSocketFactory)SSLServerSocketFactory.getDefault();
			sslServerSocket = (SSLServerSocket)sslServerSocketFactory.createServerSocket(9997);
			System.out.println("Analyst wants money...(analyst now online)");

		} catch (IOException e) {
            System.out.println("Error listening on port 9997 or listening for a connection");
			System.out.println(e.getMessage());
        }

		while(true){
			
			SSLSocket sslSocket = null;
			try {
				sslSocket = (SSLSocket)sslServerSocket.accept();
				System.out.println("Director connected. ");

			} catch (IOException e) {
				System.out.println("Error connecting to director");
				System.out.println(e.getMessage());
			}
		}

		InputStream inputStream = sslSocket.getInputStream();
		InputStreamReader inputStreamReader = new InputStreamReader(inputStream);

		// Create a buffered reader (chars -> strings) 
		// (NOTE: BufferedReader needs need a NEWLINE ended message to readLine() properly)
		BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
		// Create output stream
		OutputStream outputStream = sslSocket.getOutputStream();
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
       
        String packet = MessageFlag.A_INIT + DATA;
        Message msg = new Message();
        // send packet to director
        outputStreamWriter.write(packet);
        if (msg.getFlag() == DATA)

        inPacket = bufferedReader.readLine();
    	// extract the data from packet
		data = msg(inPacket).data;
        // decrypt the data..
        // decryptData(data);
        
	}

	// Returns TRUE iff director can handle data analysis (has analyst(s) availiable)
	private boolean initDir() throws IOException{
		try{
			// set up socket to Dir
			SSLSocketFactory sslsf = (SSLSocketFactory)SSLSocketFactory.getDefault();
			SSLSocket sslsocket = (SSLSocket)sslsf.createSocket("localhost", dirPort);
			// for incoming response from director	
			InputStream inputStream = sslsocket.getInputStream();
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
			System.out.println(inPacket);
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
