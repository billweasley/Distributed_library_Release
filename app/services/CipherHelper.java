package services;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.mindrot.jbcrypt.BCrypt;

import play.Logger;

public class CipherHelper {

	private final static String KEY = "<To fill>"; // 32 characters
																			// ,
																			// required
																			// for
																			// AES
	private final static String INIT = "<To fill>";// 16characters, required
															// for AES

	/**
	 * Create an encrypted password from a clear string.
	 *
	 * @param clearString
	 *            the clear string
	 * @return an encrypted password of the clear string
	 * @throws IllegalArgumentException
	 *             IllegalArgumentException
	 * @throws UnsupportedEncodingException
	 */
	public static String encryptBCrypt(String clearString)
			throws IllegalArgumentException, UnsupportedEncodingException {
		if (clearString == null) {
			throw new IllegalArgumentException("empty password");
		}
		String encrypted = BCrypt.hashpw(clearString, BCrypt.gensalt());
		return new String(Base64.getEncoder().encode(encrypted.getBytes("UTF-8")), "UTF-8");
	}

	/**
	 * Method to check if entered user password is the same as the one that is
	 * stored (encrypted) in the database.
	 *
	 * @param candidate
	 *            the clear text
	 * @param encrypted
	 *            the encrypted password string wrapped by Hash64 to check.
	 * @return true if the candidate matches, false otherwise.
	 * @throws UnsupportedEncodingException
	 */
	public static boolean validateBCrypt(String candidate, String encrypted) throws UnsupportedEncodingException {
		if (candidate == null) {
			return false;
		}
		if (encrypted == null) {
			return false;
		}
		encrypted = new String(Base64.getDecoder().decode(encrypted.getBytes("UTF-8")), "UTF-8");
		return BCrypt.checkpw(candidate, encrypted);
		// return new
		// String(Base64.getEncoder().encode(encrypted.getBytes("UTF-8")),"UTF-8");
	}

	public static String encryptAES(String value) {
		if (value == null)
			return null;
		return encryptAES(KEY, INIT, value);
	}

	public static String decryptAES(String value) {
		if (value == null)
			return null;
		return decryptAES(KEY, INIT, value);
	}

	public static String encryptAES(String key, String initVector, String value) {
		try {
			IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
			SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

			byte[] encrypted = cipher.doFinal(value.getBytes("UTF-8"));

			return new String(Base64.getEncoder().encode(encrypted), "UTF-8");
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return null;
	}

	public static String decryptAES(String key, String initVector, String encrypted) {
		try {
			IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
			SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
			
			byte[] original = cipher.doFinal(Base64.getDecoder().decode(encrypted));

			return new String(original, "UTF-8");
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return null;
	}


}
