package lib;

/**
 * SSL Certificate Handler
 *    > Creates consistent SSL certificate declarations.
 *    > Can be expanded to include other global properties.
 * 
 * @author Reece Notargiacomo
 * @date May 8th 2015
 * 
 */

import java.io.IOException;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

public class SSLHandler {
	private static void declareCert(String certType, String cert, String pass) {
		System.setProperty("javax.net.ssl." + certType + "Store", cert);
    	System.setProperty("javax.net.ssl." + certType + "StorePassword", pass);
	}

	public static void declareServerCert(String cert, String pass) {
		declareCert( "key", cert, pass );
		System.setProperty("http.maxConnections","40");
	}
	public static void declareClientCert(String cert, String pass) {
		declareCert( "trust", cert, pass );
	}
}
