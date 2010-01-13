package org.red5.server.net.rtmp;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2009 by respective authors (see below). All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation; either version 2.1 of the License, or (at your option) any later 
 * version. 
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this library; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 */

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.KeySpec;
import java.util.Random;

import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.apache.mina.core.buffer.IoBuffer;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.red5.server.api.Red5;
import org.red5.server.net.IHandshake;
import org.red5.server.net.rtmp.message.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates the second 1536 byte chunk in the RTMP handshake response for
 * compatibility with Flash 9,0,124,0. Clients that require this send a nonzero
 * value as the fifth byte of the handshake request.
 * 
 * <br/>
 * This class is based on the Ruby handshaking code from Takuma Mori.
 * <br />
 * 
 * @author Jacinto Shy II (jacinto.m.shy@ieee.org)
 * @author Steven Zimmer (stevenlzimmer@gmail.com)
 * @author Gavriloaie Eugen-Andrei
 * @author Ari-Pekka Viitanen
 * @author Paul Gregoire
 * @author Tiago Jacobs 
 */
public class RTMPHandshake implements IHandshake {

	protected static Logger log = LoggerFactory.getLogger(RTMPHandshake.class);	

	//for old style handshake
	public static byte[] HANDSHAKE_PAD_BYTES;
	
	private static final byte[] GENUINE_FMS_KEY = {
		(byte) 0x47, (byte) 0x65, (byte) 0x6e, (byte) 0x75, (byte) 0x69, (byte) 0x6e, (byte) 0x65, (byte) 0x20,
		(byte) 0x41, (byte) 0x64, (byte) 0x6f, (byte) 0x62, (byte) 0x65, (byte) 0x20, (byte) 0x46, (byte) 0x6c,
		(byte) 0x61, (byte) 0x73, (byte) 0x68, (byte) 0x20, (byte) 0x4d, (byte) 0x65, (byte) 0x64, (byte) 0x69,
		(byte) 0x61, (byte) 0x20, (byte) 0x53, (byte) 0x65, (byte) 0x72, (byte) 0x76, (byte) 0x65, (byte) 0x72,
		(byte) 0x20, (byte) 0x30, (byte) 0x30, (byte) 0x31, // Genuine Adobe Flash Media Server 001
		(byte) 0xf0, (byte) 0xee, (byte) 0xc2, (byte) 0x4a, (byte) 0x80, (byte) 0x68, (byte) 0xbe, (byte) 0xe8,
		(byte) 0x2e, (byte) 0x00, (byte) 0xd0, (byte) 0xd1, (byte) 0x02, (byte) 0x9e, (byte) 0x7e, (byte) 0x57,
		(byte) 0x6e, (byte) 0xec, (byte) 0x5d, (byte) 0x2d, (byte) 0x29, (byte) 0x80, (byte) 0x6f, (byte) 0xab,
		(byte) 0x93, (byte) 0xb8, (byte) 0xe6, (byte) 0x36, (byte) 0xcf, (byte) 0xeb, (byte) 0x31, (byte) 0xae};

	private static final byte[] GENUINE_FP_KEY = {
		(byte) 0x47, (byte) 0x65, (byte) 0x6E, (byte) 0x75, (byte) 0x69, (byte) 0x6E, (byte) 0x65, (byte) 0x20,
		(byte) 0x41, (byte) 0x64, (byte) 0x6F, (byte) 0x62, (byte) 0x65, (byte) 0x20, (byte) 0x46, (byte) 0x6C,
		(byte) 0x61, (byte) 0x73, (byte) 0x68, (byte) 0x20, (byte) 0x50, (byte) 0x6C, (byte) 0x61, (byte) 0x79,
		(byte) 0x65, (byte) 0x72, (byte) 0x20, (byte) 0x30, (byte) 0x30, (byte) 0x31, // Genuine Adobe Flash Player 001
		(byte) 0xF0, (byte) 0xEE, (byte) 0xC2, (byte) 0x4A, (byte) 0x80, (byte) 0x68, (byte) 0xBE, (byte) 0xE8,
		(byte) 0x2E, (byte) 0x00, (byte) 0xD0, (byte) 0xD1, (byte) 0x02, (byte) 0x9E, (byte) 0x7E, (byte) 0x57,
		(byte) 0x6E, (byte) 0xEC, (byte) 0x5D, (byte) 0x2D, (byte) 0x29, (byte) 0x80, (byte) 0x6F, (byte) 0xAB,
		(byte) 0x93, (byte) 0xB8, (byte) 0xE6, (byte) 0x36, (byte) 0xCF, (byte) 0xEB, (byte) 0x31, (byte) 0xAE};	

    private static final byte[] DH_MODULUS_BYTES = {
		(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff,
		(byte)0xc9, (byte)0x0f, (byte)0xda, (byte)0xa2, (byte)0x21, (byte)0x68, (byte)0xc2, (byte)0x34,
		(byte)0xc4, (byte)0xc6, (byte)0x62, (byte)0x8b, (byte)0x80, (byte)0xdc, (byte)0x1c, (byte)0xd1,
		(byte)0x29, (byte)0x02, (byte)0x4e, (byte)0x08, (byte)0x8a, (byte)0x67, (byte)0xcc, (byte)0x74,
		(byte)0x02, (byte)0x0b, (byte)0xbe, (byte)0xa6, (byte)0x3b, (byte)0x13, (byte)0x9b, (byte)0x22,
		(byte)0x51, (byte)0x4a, (byte)0x08, (byte)0x79, (byte)0x8e, (byte)0x34, (byte)0x04, (byte)0xdd,
		(byte)0xef, (byte)0x95, (byte)0x19, (byte)0xb3, (byte)0xcd, (byte)0x3a, (byte)0x43, (byte)0x1b,
		(byte)0x30, (byte)0x2b, (byte)0x0a, (byte)0x6d, (byte)0xf2, (byte)0x5f, (byte)0x14, (byte)0x37,
		(byte)0x4f, (byte)0xe1, (byte)0x35, (byte)0x6d, (byte)0x6d, (byte)0x51, (byte)0xc2, (byte)0x45,
		(byte)0xe4, (byte)0x85, (byte)0xb5, (byte)0x76, (byte)0x62, (byte)0x5e, (byte)0x7e, (byte)0xc6,
		(byte)0xf4, (byte)0x4c, (byte)0x42, (byte)0xe9, (byte)0xa6, (byte)0x37, (byte)0xed, (byte)0x6b,
		(byte)0x0b, (byte)0xff, (byte)0x5c, (byte)0xb6, (byte)0xf4, (byte)0x06, (byte)0xb7, (byte)0xed,
		(byte)0xee, (byte)0x38, (byte)0x6b, (byte)0xfb, (byte)0x5a, (byte)0x89, (byte)0x9f, (byte)0xa5,
		(byte)0xae, (byte)0x9f, (byte)0x24, (byte)0x11, (byte)0x7c, (byte)0x4b, (byte)0x1f, (byte)0xe6,
		(byte)0x49, (byte)0x28, (byte)0x66, (byte)0x51, (byte)0xec, (byte)0xe6, (byte)0x53, (byte)0x81,
		(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff
    };

    protected static final BigInteger DH_MODULUS = new BigInteger(1, DH_MODULUS_BYTES);

    protected static final BigInteger DH_BASE = BigInteger.valueOf(2);    	
	
	protected static final Random random = new Random();

	static {
		//get security provider
		Security.addProvider(new BouncyCastleProvider());		
	}
	
	private Mac hmacSHA256;

	private KeyAgreement keyAgreement;
	
	private byte[] handshakeBytes;
	
	private int validationScheme = -1;
	
	private int digestOffset = 0;
	
	public RTMPHandshake() {
		try {
			hmacSHA256 = Mac.getInstance("HmacSHA256");
		} catch (SecurityException e) {
			log.error("Security exception when getting HMAC", e);
		} catch (NoSuchAlgorithmException e) {
			log.error("HMAC SHA256 does not exist");
		}
		//create our server handshake bytes
		createServerHandshakeBytes();
	}
	
	/**
	 * Generates response for non-versioned connections, such as those before FP9.
	 * 
	 * @param input incoming RTMP bytes
	 * @return outgoing handshake
	 */
	public IoBuffer generateUnversionedResponse(IoBuffer input) {
		//save resource by only doing this after the first request
		if (HANDSHAKE_PAD_BYTES == null) {
    		HANDSHAKE_PAD_BYTES = new byte[Constants.HANDSHAKE_SIZE - 4];
    		//fill pad bytes
    		for (int b = 0; b < HANDSHAKE_PAD_BYTES.length; b++) {
    			HANDSHAKE_PAD_BYTES[b] = (byte) 0x00;
    		}
		}
		IoBuffer output = IoBuffer.allocate((Constants.HANDSHAKE_SIZE * 2) + 1);
		output.put((byte) 0x03);
		// set server uptime in seconds
		output.putInt((int) Red5.getUpTime() / 1000); //0x01
		output.put(RTMPHandshake.HANDSHAKE_PAD_BYTES);
		output.put(input);
		output.flip();
		return output;
	}

	/**
	 * Generates response for versioned connections.
	 * 
	 * @param input incoming RTMP bytes
	 * @return outgoing handshake
	 */
	public IoBuffer generateResponse(IoBuffer input) {
		IoBuffer output = IoBuffer.allocate((Constants.HANDSHAKE_SIZE * 2) + 1);
		input.mark();
		if (validateClient(input)) {
			input.reset();
			prepareResponse();
			input.mark();	
			byte[] newKeyPart = new byte[32];
			input.position(digestOffset);
			input.get(newKeyPart, 0, 32);			
			input.reset();
			byte[] newKey = calculateHMAC_SHA256(newKeyPart, GENUINE_FMS_KEY);
			byte[] randBytes = new byte[Constants.HANDSHAKE_SIZE - 32];
			random.nextBytes(randBytes);
			byte[] hashedBytes = calculateHMAC_SHA256(randBytes, newKey);

			//set handshake as non-encrypted (3)
			output.put((byte) 0x03);
			output.put(handshakeBytes);
			output.put(randBytes);
			output.put(hashedBytes);
			output.flip();			
		} else {
			//what shall we do if the client is not valid?
			log.warn("Invalid client connection detected");
		}
		return output;
	}

	public byte[] calculateHMAC_SHA256(byte[] input, byte[] key) {
		byte[] output = null;
		try {
			hmacSHA256.init(new SecretKeySpec(key, "HmacSHA256"));
			output = hmacSHA256.doFinal(input);
		} catch (InvalidKeyException e) {
			log.error("Invalid key", e);
		}
		return output;
	}

	public byte[] calculateHMAC_SHA256(byte[] input, byte[] key, int length) {
		byte[] output = null;
		try {
			hmacSHA256.init(new SecretKeySpec(key, 0, length, "HmacSHA256"));
			output = hmacSHA256.doFinal(input);
		} catch (InvalidKeyException e) {
			log.error("Invalid key", e);
		}
		return output;
	}
	
	private KeyPair generateKeyPair() {
		KeyPair keyPair = null;
		DHParameterSpec keySpec = new DHParameterSpec(DH_MODULUS, DH_BASE);
		try {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH");
			keyGen.initialize(keySpec);
			keyPair = keyGen.generateKeyPair();
		    keyAgreement = KeyAgreement.getInstance("DH");
		    keyAgreement.init(keyPair.getPrivate());
		} catch (Exception e) {
			log.error("Error generating keypair", e);
		}
		return keyPair;
	}

	protected static byte[] getPublicKey(KeyPair keyPair) {
		 DHPublicKey publicKey = (DHPublicKey) keyPair.getPublic();
	     BigInteger	dh_Y = publicKey.getY();
	     log.debug("public key value: " + dh_Y);
	     byte[] result = dh_Y.toByteArray();
	     log.debug("public key as bytes, len = [" + result.length + "]: " + Hex.encodeHexString(result));
	     byte[] temp = new byte[128];
	     if (result.length < 128) {
	    	 System.arraycopy(result, 0, temp, 128 - result.length, result.length);
	    	 result = temp;
	    	 log.debug("padded public key length to 128");
	     } else if(result.length > 128){
	    	 System.arraycopy(result, result.length - 128, temp, 0, 128);
	    	 result = temp;
	    	 log.debug("truncated public key length to 128");
	     }
	     return result;
	}

	@SuppressWarnings("unused")
	private static byte[] getSharedSecret(byte[] otherPublicKeyBytes, KeyAgreement keyAgreement) {
		BigInteger otherPublicKeyInt = new BigInteger(1, otherPublicKeyBytes);
		try {
			KeyFactory keyFactory = KeyFactory.getInstance("DH");
			KeySpec otherPublicKeySpec = new DHPublicKeySpec(otherPublicKeyInt, DH_MODULUS, DH_BASE);
			PublicKey otherPublicKey = keyFactory.generatePublic(otherPublicKeySpec);
		    keyAgreement.doPhase(otherPublicKey, true);
		} catch(Exception e) {
			log.error("Error getting shared secret", e);
		}
	    byte[] sharedSecret = keyAgreement.generateSecret();
	    log.debug("shared secret (" + sharedSecret.length + " bytes): " + Hex.encodeHexString(sharedSecret));
	    return sharedSecret;
	}	
	
	private boolean validateClient(IoBuffer input) {
		byte[] pBuffer = new byte[input.remaining()];
		//put all the input bytes into our buffer
		input.get(pBuffer, 0, input.remaining());
		
	    if (validateClientScheme(pBuffer, 0)) {
	        validationScheme = 0;
			log.debug("Selected scheme: 0");
	        return true;
	    }
	    if (validateClientScheme(pBuffer, 1)) {
	        validationScheme = 1;
			log.debug("Selected scheme: 1");
	        return true;
	    }
	    log.error("Unable to validate client");
	    return false;
	}
	
	private boolean validateClientScheme(byte[] pBuffer, int scheme) {
		switch (scheme) {
			case 0:
				digestOffset = getDigestOffset0(pBuffer);
				break;
			case 1:
				digestOffset = getDigestOffset1(pBuffer);
				break;
			default:
				log.error("Unknown scheme: {}", scheme);
		}   
		log.debug("Scheme: {} digest offset: {}", scheme, digestOffset);

	    byte[] tempBuffer = new byte[Constants.HANDSHAKE_SIZE - 32];
	    //memcpy(tempBuffer, pBuffer, clientDigestOffset);
	    System.arraycopy(pBuffer, 0, tempBuffer, 0, digestOffset);
	    //memcpy(tempBuffer + clientDigestOffset, pBuffer + clientDigestOffset + 32, Constants.HANDSHAKE_SIZE - clientDigestOffset - 32);
	    System.arraycopy(pBuffer, digestOffset + 32, tempBuffer, digestOffset, Constants.HANDSHAKE_SIZE - digestOffset - 32);	    

	    byte[] tempHash = calculateHMAC_SHA256(tempBuffer, GENUINE_FP_KEY, 30);
	    log.debug("Temp: {}", Hex.encodeHexString(tempHash));

	    boolean result = true;
	    for (int i = 0; i < 32; i++) {
	    	//log.trace("Digest: {} Temp: {}", (pBuffer[digestOffset + i] & 0x0ff), (tempHash[i] & 0x0ff));
	        if (pBuffer[digestOffset + i] != tempHash[i]) {
	            result = false;
	            break;
	        }
	    }

	    return result;
	}	
	
	private int getDHOffset0() {
		int offset = (handshakeBytes[1532] & 0x0ff) + (handshakeBytes[1533] & 0x0ff) + (handshakeBytes[1534] & 0x0ff) + (handshakeBytes[1535] & 0x0ff);
	    offset = offset % 632;
	    offset = offset + 772;
	    if (offset + 128 >= 1536) {
	    	 log.error("Invalid DH offset");
	    }
	    return offset;
	}
	
	private int getDHOffset1() {
		int offset = (handshakeBytes[768] & 0x0ff) + (handshakeBytes[769] & 0x0ff) + (handshakeBytes[770] & 0x0ff) + (handshakeBytes[771] & 0x0ff);
	    offset = offset % 632;
	    offset = offset + 8;
	    if (offset + 128 >= 1536) {
	    	 log.error("Invalid DH offset");
	    }
	    return offset;
	}	
	
	private int getDigestOffset0(byte[] pBuffer) {
		int offset = (pBuffer[8] & 0x0ff) + (pBuffer[9] & 0x0ff) + (pBuffer[10] & 0x0ff) + (pBuffer[11] & 0x0ff);
	    offset = offset % 728;
	    offset = offset + 12;
	    if (offset + 32 >= 1536) {
	        log.error("Invalid digest offset");
	    }
	    return offset;
	}

	private int getDigestOffset1(byte[] pBuffer) {
		int offset = (pBuffer[772] & 0x0ff) + (pBuffer[773] & 0x0ff) + (pBuffer[774] & 0x0ff) + (pBuffer[775] & 0x0ff);
	    offset = offset % 728;
	    offset = offset + 776;
	    if (offset + 32 >= 1536) {
	        log.error("Invalid digest offset");
	    }
	    return offset;
	}

	/**
	 * Creates the servers handshake bytes
	 */
	private void createServerHandshakeBytes() {
		handshakeBytes = new byte[Constants.HANDSHAKE_SIZE];
		//timestamp
		handshakeBytes[0] = 0;
		handshakeBytes[1] = 0;
		handshakeBytes[2] = 0;
		handshakeBytes[3] = 0;
		//version
		handshakeBytes[4] = 3;
		handshakeBytes[5] = 1;
		handshakeBytes[6] = 0;
		handshakeBytes[7] = 0;
		//random bytes
		
		//slow way
		//StringBuilder sb = new StringBuilder();
		for (int i = 8; i < Constants.HANDSHAKE_SIZE; i++) {
			handshakeBytes[i] = (byte) (random.nextInt(255) & 0xff);
			//sb.append(handshakeBytes[i] & 0x0ff);
			//sb.append(", ");
		}
		//log.debug("Handshake bytes:\n{}\n", sb.toString());
		
		//faster
		//byte[] rndBytes = new byte[3064];
		//random.nextBytes(rndBytes);		
		//copy random bytes into our handshake array
		//System.arraycopy(rndBytes, 0, handshakeBytes, 8, 3064);	
	}
	
	/**
	 * Gets the DH offset in the handshake bytes array based on validation scheme
	 * Generates DH keypair
	 * Adds public key to handshake bytes
	 */
	private void prepareResponse() {
		//get dh offset
		int dhOffset = -1;
		switch (validationScheme) {
			case 1:
				dhOffset = getDHOffset1();
				break;
			default:
				log.debug("Scheme 0 will be used for DH offset");
			case 0:
				dhOffset = getDHOffset0();
		}   		
		//create keypair
		KeyPair keys = generateKeyPair();
		//get public key
		byte[] publicKey = getPublicKey(keys);
		//add to handshake bytes
		System.arraycopy(publicKey, 0, handshakeBytes, dhOffset, 128);
	}

	public byte[] getHandshakeBytes() {
		return handshakeBytes;
	}
	
}
