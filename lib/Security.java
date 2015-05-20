package lib;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

public class Security {

	public static void declareServerCert(String cert, String pass) {
		System.setProperty("javax.net.ssl.keyStore", cert);
    	System.setProperty("javax.net.ssl.keyStorePassword", pass);
	}
	public static void declareClientCert(String cert) {
		System.setProperty("javax.net.ssl.trustStore", cert);
	}

	protected PublicKey KeyFromString(String publicKeyString) {
		try {
			byte[] keyBytes = Base64.decode(publicKeyString.getBytes("utf-8"));
			X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			return keyFactory.generatePublic(spec);
		} catch (Exception e) {
			System.out.println(e);
			return null;
		}
	}
	
	protected String StringFromKey(PublicKey pubKey) {
		return Base64.encodeBytes(pubKey.getEncoded());
	}
	
	protected String encrypt(String unencrypted, PublicKey pub) {
		try {
			byte[] bytes = unencrypted.getBytes();
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.ENCRYPT_MODE, pub);
			byte[] cipherData = cipher.doFinal(bytes);
		  	return Base64.encodeBytes(cipherData);
		}catch(Exception e) {
			return null;
		}
	}
	
	protected String decrypt(String encrypted, PrivateKey priv) {

		try {
			byte[] b = Base64.decode(encrypted);
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.DECRYPT_MODE, priv);
			byte[] cipherData = cipher.doFinal(b);
		  	return new String(cipherData);
		}catch(Exception e) {
			return null;
		}
	}
}
