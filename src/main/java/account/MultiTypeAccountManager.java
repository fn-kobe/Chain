package account;

import util.FileLogger;

import java.util.*;

public class MultiTypeAccountManager {
    static org.apache.log4j.Logger logger = FileLogger.getLogger();
    private HashMap<String, Double> accountBalanceList;
    private HashMap<String, Double> accountFrozenList;
    String type;

    public MultiTypeAccountManager(String type) {
        accountBalanceList = new HashMap<>();
        accountFrozenList = new HashMap<>();
        this.type = type;
    }

    public Set<String> getAllAccount(){
        return accountBalanceList.keySet();
    }

    public synchronized Double getBalance(String address) {
        //DebugOnlyAccount();
        String shortAddress = AccountManager.getShortAddress(address);
        if (!accountBalanceList.containsKey(shortAddress)) {
            logger.error(String.format("[AccountManager] Address '%s' doesn't exist with type %s in " + this, shortAddress, type));
            accountBalanceList.put(shortAddress, Double.valueOf(0));
            return Double.valueOf(0);
        }
        return accountBalanceList.get(shortAddress);
    }

    public void reset() {
        accountBalanceList = new HashMap<>();
        accountFrozenList = new HashMap<>();
    }

    public boolean changeValue(String address, Double value) {
        //DebugOnly(address, value, "changevalue");
        if (value >= 0) return addValue(address, value);

        return subValue(address, -value);
    }

    public boolean addValue(String address, int value) {
        return addValue(address, Double.valueOf(value));
    }

    public synchronized boolean addValue(String address, Double value) {
        //DebugOnly();
        String shortAddress = AccountManager.getShortAddress(address);
        if (value < 0) {
            logger.warn(String.format("[AccountManager][addValue] Value: %f is negative", value));
            return false;
        }
        if (!accountBalanceList.containsKey(shortAddress)) {
            logger.warn(String.format("[AccountManager][%s] Address '%s' doesn't exist, try to create it " + this, type, shortAddress));
            accountBalanceList.put(shortAddress, value);
        } else {
            accountBalanceList.replace(shortAddress, accountBalanceList.get(shortAddress) + value);
        }
        return true;
    }

    public boolean subValue(String address, int value) {
        return subValue(address, Double.valueOf(value));
    }

    public synchronized boolean subValue(String address, Double value) {
        //DebugOnly();
        if (!canSubValue(address, value)) return false;
        String shortAddress = AccountManager.getShortAddress(address);
        accountBalanceList.replace(shortAddress, accountBalanceList.get(shortAddress) - value);
        return true;
    }

    public synchronized boolean freezeValue(String address, Double value) {
        if (!subValue(address, value)) return false;
        if (accountFrozenList.containsKey(address)) {
            accountFrozenList.replace(address, accountFrozenList.get(address) + value);
        } else {
            accountFrozenList.put(address, value);
        }
        return true;
    }

    public synchronized Double getFreezeValue(String address) {
        if (accountFrozenList.containsKey(address)){
            return accountFrozenList.get(address) ;
        }
        logger.warn(String.format("[AccountManager] Address '%s' doesn't have frozen asset", address));
        return Double.valueOf(0);
    }


    public synchronized boolean unFreezeValue(String address, Double value) {
        if (accountFrozenList.get(address) >= value) {
            addValue(address, value);
            accountFrozenList.replace(address, accountFrozenList.get(address) - value);
            return true;
        }

        logger.error(String.format("[AccountManager][unFreezeValue] Frozen value: %f is not enough", accountFrozenList.get(address)));
        return false;
    }

    public boolean canSubValue(String address, double value) {
        //DebugOnly();
        String shortAddress = AccountManager.getShortAddress(address);
        if (value < 0) {
            logger.warn(String.format("[AccountManager][canSubValue] Value: %f is negative", value));
            return false;
        }
        if (!accountBalanceList.containsKey(shortAddress)) {
            logger.warn(String.format("[AccountManager] Address '%s' doesn't exist, skip subtract", shortAddress));
            return false;
        }
        if (accountBalanceList.get(shortAddress) - value < 0) {
            logger.warn(String.format("[AccountManager] Balance is not enough of %s to value %f", shortAddress, value));
            return false;
        }
        return true;
    }

    public boolean canTransferValue(String address, double value) {
        return canSubValue(address, value);
    }

    // If can transfer, return true and the value is subtracted
    public boolean transferValue(String from, String to, int value) {
        return transferValue(from, to, Double.valueOf(value));
    }

    public boolean transferValue(String from, String to, Double value) {
        //DebugOnly();
        if (value < 0) {
            logger.warn(String.format("[AccountManager][transferValue] Value: %d is negative", value));
            return false;
        }
        if (!subValue(from, value)) return false;
        return addValue(to, value);
    }

    void DebugOnly(){
        System.out.println("[AccountManager] address: " + this);
        Thread.dumpStack();
    }

    void DebugOnlyAccount(){
        System.out.println("[AccountManager] address: " + this);

        System.out.println("[AccountManager] current account " + accountBalanceList.toString());
    }

    void DebugOnly(String address){
        System.out.println("[AccountManager] address: " + this);

        System.out.println(address + " with balance " + getBalance(address));
        Thread.dumpStack();
    }

    void DebugOnly(String address, Double value, String op){
        System.out.println("[AccountManager] address: " + this);

        System.out.println(address + " with balance " + getBalance(address) + " with op: " + op + " with value: " + value);
        Thread.dumpStack();
    }

    // For test or onitor purpose
    public String dump(){
        String d = "<" + type + ">\n";
        Iterator it = accountBalanceList.entrySet().iterator();
        while (it.hasNext()){
            Map.Entry entry = (Map.Entry) it.next();
            d += entry.getKey() + " : " + entry.getValue() + "\n";
        }
        d += "</" + type + ">\n";
        return d;
    }

}
