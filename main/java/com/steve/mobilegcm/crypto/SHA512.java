package com.steve.mobilegcm.crypto;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA512 {

    private static String convertToHex(byte[] data) {
        StringBuilder stringBuilder=new StringBuilder();
        for(byte b:data) {
            int halfbyte=(b>>>4)&0x0F;
            int two_halfs=0;
            do {
                stringBuilder.append((0 <= halfbyte) && (halfbyte <= 9) ? (char) ('0' + halfbyte) : (char) ('a' + (halfbyte - 10)));
                halfbyte=b&0x0F;
            }
            while(two_halfs++<1);
        }
        return stringBuilder.toString();
    }

    public static String sha512(String textToBeEncrypted)throws NoSuchAlgorithmException,UnsupportedEncodingException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-512");
        messageDigest.update(textToBeEncrypted.getBytes("iso-8859-1"), 0, textToBeEncrypted.length());
        byte[] sha512Hash = messageDigest.digest();
        return convertToHex(sha512Hash);
    }

}
