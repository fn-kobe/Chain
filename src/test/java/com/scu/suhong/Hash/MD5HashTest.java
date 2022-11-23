package com.scu.suhong.Hash;

import com.scu.suhong.transaction.Transaction;
import junit.framework.TestCase;
import util.StringHelper;

import java.security.NoSuchAlgorithmException;

public class MD5HashTest extends TestCase {

    public void testGetMD5() throws NoSuchAlgorithmException {
        String result;
        result = MD5Hash.getValue("");
        System.out.println(result);

        result = MD5Hash.getValue("Original block hash");
        System.out.println(result);

        result = MD5Hash.getValue("su Henry");
        System.out.println(result);
    }

    public void testGetValue() throws NoSuchAlgorithmException {
        Transaction transaction = new Transaction();
        String msg = "abcdedft-228249940";
        transaction.setData(msg);
        transaction.setHash();
        String trMd51 = StringHelper.byteArrayToHexString(MD5Hash.getValue(transaction.getJson().toString().getBytes()));

        msg = "abcdedft-670482351";
        transaction.setData(msg);
        transaction.setHash();
        String trMd52 = StringHelper.byteArrayToHexString(MD5Hash.getValue(transaction.getJson().toString().getBytes()));

        assert !trMd51.equals(trMd52);
    }
}