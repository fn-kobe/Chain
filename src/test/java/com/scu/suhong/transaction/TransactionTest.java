package com.scu.suhong.transaction;

import com.scu.suhong.dynamic_definition.DynamicalAsset;
import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONObject;
import org.junit.Test;
import util.RandomHelper;

public class TransactionTest {
    @Test
    public void testTransactionSize(){
        int randomLow = 40;
        int randomHigh = 80;
        RandomHelper randomHelper = new RandomHelper(randomLow, randomHigh);

        int repeatTimes = 40;
        for (int i = 0; i < repeatTimes; ++i) {
            randomGenerateNormalTransaction(randomHelper);

        }

        randomHelper = new RandomHelper(randomLow, randomHigh);
        for (int i = 0; i < repeatTimes; ++i) {
            randomGenerateDynamicalTransaction(randomHelper);
        }
    }

    private void randomGenerateNormalTransaction(RandomHelper randomHelper) {
        int length = randomHelper.getNumber();
        String from = "123?0xabc120";
        String to = "123?0xabc121";
        int value = 6;
        String toSeparator = ":";
        Transaction transaction = new Transaction();
        transaction.setFrom(from);
        transaction.setToAndValue(to + toSeparator + value, 6);
        String data = RandomStringUtils.randomAlphanumeric(length - from.length() - to.length());
        transaction.setData(data);
        transaction.setId();

        System.out.println(transaction.Dump());
        System.out.println("[Test] Normal transaction size: " + transaction.getJson().toString().length());
    }

    private void randomGenerateDynamicalTransaction(RandomHelper randomHelper) {
        int length = randomHelper.getNumber();
        String owner = "0xabc120";
        DynamicalAsset transaction = new DynamicalAsset();
        transaction.setOwner(owner);

        RandomHelper randomHelperCode = new RandomHelper(0,20);
        int codeLength = randomHelperCode.getNumber();
        String code = RandomStringUtils.randomAlphanumeric(length - owner.length());
        transaction.setCode(code);
        String data = RandomStringUtils.randomAlphanumeric(length - owner.length() - code.length());
        transaction.setData(data);
        transaction.setId();

        System.out.println(transaction.Dump());
        System.out.println("[Test] Dynamical transaction size: " + transaction.getJson().toString().length());
    }

    @Test
    public void testParseMultiToParameter() {
        String from = "0xabc120";
        String to1 = "0xabc121";
        int value1 = 1;
        String to2 = "0xabc122";
        int value2 = 2;
        String to3 = "0xabc123";
        int value3 = 3;
        String toSeparator = ":";
        Transaction t1 = new Transaction();
        t1.setFrom(from);
        t1.setToAndValue(to1 + toSeparator + value1 + toSeparator + to2 + toSeparator + value2 + toSeparator + to3 + toSeparator + value3, 6);
        assert t1.getToList().size() == 3;
        System.out.println(t1.Dump());

        Transaction t2 = new Transaction();
        t2.setToAndValue(to1+to2, 2);
        assert t2.getToList().size() == 1;

        Transaction t3 = new Transaction();
        t3.setToAndValue(to1 + toSeparator + value1 + toSeparator + to2 + toSeparator + value2 + toSeparator + to3 + toSeparator + value3,
                4);
        assert t3.getToList().size() == 0;

        Transaction t4 = new Transaction();
        t4.setToAndValue(to1 + toSeparator + value1 + toSeparator + to2 + toSeparator + value2 + toSeparator + to3 + toSeparator + value3, 4);
        assert t4.getToList().size() == 0;

        JSONObject t1Json = t1.getJson();
        System.out.println(t1Json.toString());
        Transaction t5 = Transaction.createFromJson(t1Json);
        assert t5.getToList().size() == 3;
    }
}