package com.scu.suhong.transaction;

import account.AccountManager;
import com.scu.suhong.Hash.MD5Hash;
import org.apache.log4j.Logger;
import util.FileLogger;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Set;

public class ToValuePair {
    static Logger logger = FileLogger.getLogger();

    HashMap<String, Integer> valueMap = new HashMap<>();
    String multiReceiverSeparator = "_";

    String toListString = "";

    int value = -1;
    String to = "";

    void clear() {
        to = "";
        value = -1;
        valueMap.clear();
    }

    public void setToAndValue(String to, String value) {
        if (null == value || value.isEmpty()) {
            clear();
        } else {
            setToAndValue(to, Integer.parseInt(value));
        }
    }

    public String getToString() {
        return toListString.isEmpty() ? to : toListString;
    }

    public void setToAndValue(String to, int value) {
        clear();
        if (null == to || to.isEmpty()){
            System.out.println("[ToValuePair] Receiver address is empty and we skip setting it");
            return;
        }

        if (isMultiReceiver(to)) {
            if (!(valueMap = parseMultiToParameter(to, value)).isEmpty()) {
                toListString = AccountManager.getFullAddress(to);
                this.value = value;
            } else {
                logger.error("[ToValuePair] Bad format of multi receiver" + to + " with value " + value);
            }
        } else {
            to = AccountManager.getFullAddress(to);
            this.to = to;
            this.value = value;
            valueMap.put(to, value);
        }
    }

    public boolean isMultiReceiver(String to) {
        return to.contains(multiReceiverSeparator);
    }

    public Set<String> getToList() {
        return valueMap.keySet();
    }

    public String getTo() {
        return to;
    }

    public int getValue() {
        return value;
    }

    public int getValue(String receiver) {
        if (!valueMap.isEmpty()) {
            if (valueMap.containsKey(receiver)) {
                return valueMap.get(receiver);
            }
        }
        return 0;
    }

    public String Dump() {
        String r = "";
        if (!to.isEmpty()) r = "To: " + to;
        else {
            for (String to : valueMap.keySet()) {
                r += "To: " + to;
                r += " with value " + valueMap.get(to);
                r += "\n";
            }
            r = r.substring(0, r.length() > 0 ? r.length() - 1 : 0); // remove the last line changer
        }
        return r;
    }

    public String calculateDataHash() {
        String h = "";
        try {
            for (String to : valueMap.keySet()) {
                h = MD5Hash.getValue(to + valueMap.get(to));
            }
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Cannot find MD5 algorithm in makeHash");
            e.printStackTrace();
        }
        return h;
    }

    public HashMap parseMultiToParameter(String to, int value) {
        HashMap<String, Integer> r = new HashMap<>();
        String[] toParameters = to.split(multiReceiverSeparator);
        if (0 != toParameters.length % 2) {
            logger.error("[Transaction] Invalid multiple receiver value pair " + to);
            return r;
        }
        String receiver;
        int tempValue = 0;
        int toTalValue = 0;
        for (int i = 0; i < toParameters.length; i += 2) {//skip receiver and value
            receiver = AccountManager.getFullAddress(toParameters[i]);
            tempValue = Integer.parseInt(toParameters[i + 1]);
            toTalValue += tempValue;
            r.put(receiver, tempValue);
        }
        if (toTalValue != value) {
            logger.error("[Transaction] total value in to list is not same to value in transaction");
            r.clear();
            return r;
        }
        return r;
    }

}
