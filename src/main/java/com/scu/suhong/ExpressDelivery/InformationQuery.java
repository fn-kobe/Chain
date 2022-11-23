package com.scu.suhong.ExpressDelivery;

import util.ThreadHelper;
import util.TimeHelper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

// refer to https://www.baeldung.com/java-http-request
public class InformationQuery {
    final static String internalAddress = "http://q.kdpt.net/api";

    //test
    static int c = 0;
    public static boolean doesContainFeatureString(String address, Map<String, String> parameters, String features) {
        String result;
        try {
            result = getResult(address, parameters);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return result.contains(features);
    }

    public static String isFeatureStringOK(String address, Map<String, String> parameters, String features) {
        String result;
        try {
            result = getResult(address, parameters);
        } catch (IOException e) {
            e.printStackTrace();
            return "ERROR";
        }

        if (result.contains(features)){
            return "OK";
        } else {
            return "OnGoing";
        }
    }

    public static String getResult(String address, Map<String, String> parameters ) throws IOException {
        System.out.println("[Condition][Info] begin to sleep one seconds to avoid kuaidi100 complains too quickly");
        ThreadHelper.safeSleep(1000);
        String realAddress = address.isEmpty()? internalAddress : address;
        URL url = new URL(realAddress);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        con.setDoOutput(true);
        DataOutputStream out = new DataOutputStream(con.getOutputStream());
        parameters.put("id", "XDB2g2sjbns911ow972aNo0I_1249970652");
        parameters.put("order", "desc");
        parameters.put("format", "kuaidi100");
        parameters.put("show", "json");
        out.writeBytes(getParamsString(parameters));
        out.flush();
        out.close();

        con.setConnectTimeout(5000);
        con.setReadTimeout(5000);

        int status = con.getResponseCode();
        System.out.println("The statue code is  " + status);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        con.disconnect();

        System.out.printf("[InformationQuery][Debug][%s] The information from query is %s\n" , TimeHelper.getCurrentTimeUsingCalendar()
                , content.toString());
        return content.toString();
    }

    public static String getParamsString(Map<String, String> params)
            throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            result.append("&");
        }

        String resultString = result.toString();
        return resultString.length() > 0
                ? resultString.substring(0, resultString.length() - 1)
                : resultString;
    }
}
