package util;

import org.apache.logging.log4j.core.util.KeyValuePair;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class StringHelper {
    static org.apache.log4j.Logger logger = FileLogger.getLogger();

    private final static String[] hexDigits = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"};

    final static String getResourcePeerAddressMsg = "getResourcePeerAddress";
    final static String queryTopBlockMsg = "QueryTopBlockMsg";
    final static String queryTopBlockMsgSeparator = ":";
    static String hostIP = null;
    final static int defaultPort = 9094;

    final static String getResourcePeerAddressMsgSeparator = ":";

    public static String getQueryTopBlockMsg() {
        return queryTopBlockMsg;
    }

    public static String getQueryTopBlockMsgSeparator() {
        return queryTopBlockMsgSeparator;
    }

    public static String byteArrayToHexString(byte[] b) {
        StringBuffer resultSb = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            resultSb.append(byteToHexString(b[i]));
        }
        return resultSb.toString();
    }

    public static byte[] hexStringToByteArray(String s) {
        byte[] byteArray = s.getBytes();
        for (int i = 0; i < s.length(); i++) {
            byteArray[i] = (byte) (byteArray[i] - '0');
        }
        return byteArray;
    }

    static public String generateRandomString(char repeatChar, int range, int start) {
        int l = (int) (Math.random() * range + start);
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < l; ++i) {
            stringBuilder.append(repeatChar);
        }
        return stringBuilder.toString();
    }

    static public char generateRandomChar() {
        int l = (int) (Math.random() * 25) + 'a';
        return (char) l;
    }

    static public String generateRandomString(int range, int start) {
        int l = (int) (Math.random() * range + start);
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < l; ++i) {
            stringBuilder.append(generateRandomChar());
        }
        return stringBuilder.toString();
    }

    static public String generateIntString(int range, int start) {
        return String.valueOf((int) (Math.random() * range + start));
    }

    static public int generateInt(int range, int start) {
        return (int) (Math.random() * range + start);
    }

    private static String byteToHexString(byte b) {
        int n = b;
        if (n < 0)
            n = 256 + n;
        int d1 = n / 16;
        int d2 = n % 16;
        return hexDigits[d1] + hexDigits[d2];
    }

    static public String getAddressPortSeparator(){ return ":";}

    static public String getPoWConsensusFlag(){ return "PoW";}

    static public boolean isPoWConsensusFlag(String flag){ return flag.equalsIgnoreCase(getPoWConsensusFlag());}

    static public String getPoSConsensusFlag(){ return "PoS";}

    static public boolean isPoSConsensusFlag(String flag){ return flag.equalsIgnoreCase(getPoSConsensusFlag());}

    static public AddressPort getAddressPort(String addressPort){
        return getAddressPort(addressPort, defaultPort);
    }

    static public AddressPort getAddressPort(String addressPort, int defaultPort){
        AddressPort r = new AddressPort();
        if (null == addressPort || addressPort.isEmpty()) return r;

        String[] addressPortArray = addressPort.split(getAddressPortSeparator());
        int port = defaultPort;
        if (addressPortArray.length > 1){
            port = Integer.valueOf(addressPortArray[1]);
        }

        r.setAddressPort(addressPortArray[0], port);
        return r;
    }

    static public int getRandomNumberByHash(String hash) {
        int r = 0;
        for (byte c : hash.getBytes()) r += c;
        return r;
    }

    static public int getDefaultPort(){return defaultPort;}

    static public List<AddressPort> loadIPAndPortFromFile(String fileName) {
        List<AddressPort> peerAddressPortList = new ArrayList<>();
        FileReader fileReader = null;
        BufferedReader br = null;
        try {
            fileReader = new FileReader(new File(fileName));
            br = new BufferedReader(fileReader);
            String line = null;
            // if no more lines the readLine() returns null
            while ((line = br.readLine()) != null) {
                // reading lines until the end of the file
                line.trim();
                if (line.isEmpty()) continue;
                AddressPort addressPort = getAddressPort(line,defaultPort);
                if (addressPort.isValid()){
                    peerAddressPortList.add(addressPort);
                } else {
                    logger.info("[StringHelper] Invalid IP " + line);
                }
            }
        } catch (FileNotFoundException e) {
            logger.info("[StringHelper][WARN] exception happened when try to get IP from file " + fileName);
        } catch (IOException e) {
            logger.info("[StringHelper][WARN] exception happened when try to get IP from file " + fileName);
        } finally {
            safeClose(fileReader, br);
        }

        return peerAddressPortList;
    }


    static public List<String> loadDI(String fileName) {
        List<String> peerAddressList = new ArrayList<>();

        FileReader fileReader = null;
        BufferedReader br = null;
        try {
            fileReader = new FileReader(new File(fileName));
            br = new BufferedReader(fileReader);
            String line = null;
            // if no more lines the readLine() returns null
            while ((line = br.readLine()) != null) {
                // reading lines until the end of the file
                line.trim();
                if (isValidIP(line)) {
                    logger.info("[StringHelper] Find IP address " + line);
                    peerAddressList.add(line);
                } else {
                    logger.info("[StringHelper] Invalid IP " + line);
                }
            }
        } catch (FileNotFoundException e) {
            logger.info("[StringHelper][WARN] exception happened when try to get IP from file " + fileName);
        } catch (IOException e) {
            logger.info("[StringHelper][WARN] exception happened when try to get IP from file " + fileName);
        } finally {
            safeClose(fileReader, br);
        }

        return peerAddressList;
    }

    private static void safeClose(FileReader fileReader, BufferedReader br) {
        try {
            if (null != br) {
                br.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (null != fileReader) {
                fileReader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getIPv4String(String s) {
        int start = -1;
        int end = -1;
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (c != '.' && (c < '0' || c > '9')) {
                end = i;
                break;
            } else if (-1 == start) {
                start = i;
            }
        }
        if (start > end) end = s.length(); // string end is still number
        if (-1 == start) return "";
        return s.substring(start, end);
    }

    static public boolean isGetTopBlockRequest(byte[] msg) {
        String msgText = new String(msg);

        if (msgText.contains(queryTopBlockMsg)) {
            return true;
        }
        return false;
    }

    static public Long safeGetEpocFromString(String s){
        Long currentEpoc = TimeHelper.getEpoch();
        System.out.println("[StringHelper] Current epoc " + currentEpoc);
        return safeGetLongFromString(s, currentEpoc);
    }

    static public Long safeGetLongFromString(String s, Long defaultValue){
        Long r = defaultValue;
        try {
            r = Long.valueOf(s);
        } catch (NumberFormatException e) {
            System.out.printf("%s is not number format, using default value %s\n", s, defaultValue.toString());;
            r = defaultValue;
        }
        return r;
    }

    public static boolean isValidIP(String ip) {
        if (ip == null || ip.isEmpty()) return false;
        ip = ip.trim();
        if ((ip.length() < 6) & (ip.length() > 15)) return false;

        try {
            Pattern pattern = Pattern.compile("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
            Matcher matcher = pattern.matcher(ip);
            return matcher.matches();
        } catch (PatternSyntaxException ex) {
            return false;
        }
    }

    public static List splitCommandString(String commandString) {
        return Arrays.asList(commandString.split("\\s+"));
    }
   public static Map<String, String> getKeyValueParameters(String keyValueString,
                                                           String pairsSeparator, String keyValueSeparator){
     Map<String, String> r = new HashMap<>();
     if (null == keyValueSeparator || keyValueString.isEmpty()) return r;// empty

     String [] keyValuePairList = keyValueString.split(pairsSeparator);

     for (int i = 0; i< keyValuePairList.length; ++i){
       KeyValuePair kv = getKeyValue(keyValuePairList[i], keyValueSeparator);
       if (null == kv){
         System.out.println("[StringHelper][ERROR] Invalid key value parameters");
          return new HashMap<>();
        }
        r.put(kv.getKey(), kv.getValue());
     }

     return r;
   }

   public static String getValueFromKeyValueParameters(String key, String keyValueString,
                                                           String pairsSeparator, String keyValueSeparator){
     Map<String, String> r = getKeyValueParameters(keyValueString, pairsSeparator, keyValueSeparator);
     if (null == r) return "";
     if (!r.containsKey(key)) return "";

     return r.get(key);
   }

   static KeyValuePair getKeyValue(String originalString, String separator){
     String[] keyValue = originalString.split(separator);
     if (2 != keyValue.length){
       System.out.printf("[StringHelper][ERROR] Invalid pair '%s'\n", originalString);
       return null;
     }

     return new KeyValuePair(keyValue[0], keyValue[1]);
   }
 }
