package com.scu.suhong.transaction;

import account.AccountManager;
import com.scu.suhong.Hash.MD5Hash;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import util.FileLogger;
import util.JSONObjectHelper;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Condition {
    static Logger logger = FileLogger.getLogger();

    private String from = "";
    private String to = "";
    private int value = 0;

    ToValuePair toValuePair = new ToValuePair();

    public Condition(String from, String to, int value) {
        commonInit(from, to, value);
    }

    public Condition(String from, String to, String valueString) {
        commonInit(from, to, Integer.parseInt(valueString));
    }

    void commonInit(String from, String to, int value){
        this.from = AccountManager.getFullAddress(from);
        setToAndValue(to, value);
        this.value = value;
        this.to = to;
    }

    public String getFrom() {
        return from;
    }

    public String getFrom(String replaceSymbol) {
        return from.replace(AccountManager.getAddressConnectSymbol(), replaceSymbol);
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return toValuePair.getTo();
    }

    public void setToAndValue(String to, int value) {
        toValuePair.setToAndValue(to, value);
    }

    public int getValue() {
        return toValuePair.getValue();
    }

    public int getValue(String receiver) {
        return toValuePair.getValue(receiver);
    }

    public String Dump() {
        String dump = "<condition>\n";
        dump += "from:" + from + "\n";
        dump += toValuePair.Dump() + "\n";
        dump += "value:" + toValuePair.getValue() + "\n";
        dump += "</condition>\n";
        return dump;
    }

    public JSONObject getJson() {
        JSONObject json = new JSONObject();
        json.put("from", from);
        json.put("to", to);
        json.put("value", value);
        return json;
    }

    static public Condition createFromJson(JSONObject object) {
        Condition condition = new Condition(JSONObjectHelper.safeGetString(object, "from"),
                JSONObjectHelper.safeGetString(object, "to"), JSONObjectHelper.safeGetInt(object, "value"));
        return condition;
    }

    public String calculateDataHash() {
        String h = "";
        try {
            h += MD5Hash.getValue(from);
            h += MD5Hash.getValue(to);
            h = MD5Hash.getValue(String.valueOf(value + h));
            return h;
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Cannot find MD5 algorithm in makeHash");
            e.printStackTrace();
        }
        return null;
    }

    public ToValuePair geToValuePair() {
        return toValuePair;
    }
}
