package com.scu.suhong.Hash;

import util.StringHelper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Hash {
    public static String getValue(String msg) throws NoSuchAlgorithmException {
        return StringHelper.byteArrayToHexString(getValue(msg.getBytes()));
    }

    public static byte[] getValue(byte[] msg) throws NoSuchAlgorithmException {
        //System.out.println("[Debug] The value for MD5 is: " + msg);
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(msg);
        byte[] md5Byte = md5.digest();
        //System.out.println("[Debug] The hash value is: " + byteArrayToHexString(md5Byte));
        return md5Byte;
    }

    public static String safeGetValue(byte[] msg) {
        //System.out.println("[Debug] The value for MD5 is: " + msg);
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }
        md5.update(msg);
        byte[] md5Byte = md5.digest();
        //System.out.println("[Debug] The hash value is: " + byteArrayToHexString(md5Byte));
        return String.valueOf(md5Byte);
    }

    public static String getValue(String previousHash, String blockHash, int blockNounce) throws NoSuchAlgorithmException {
        String hashValue = "";
        if (null != previousHash) {
            hashValue += previousHash;
        }
        if (null != blockHash){
            hashValue += blockHash;
        }
        hashValue += blockNounce;
        return getValue(hashValue);
    }
}
