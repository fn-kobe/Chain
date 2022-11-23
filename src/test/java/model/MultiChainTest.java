package model;

import junit.framework.TestCase;
import util.ThreadHelper;

public class MultiChainTest extends TestCase {
    public void testMultiChainWithSpecialTxWithInvalidBlockInLightWithBigDifferentPeriod(){
        OneChain firstChain = new OneChain();
        OneChain secondChain = new OneChain();

        firstChain.setMiningMeanTime(2);
        secondChain.setMiningMeanTime(16);

        OneChainService firstChainService = new OneChainService();
        firstChainService.startService(firstChain, OneChain.getMiningTxNumber() * 3);

        OneChainService secondChainService = new OneChainService();
        secondChainService.startService(secondChain);

        ThreadHelper.safeSleepSecond(2);

        firstChain.beginConditionCountBlock("abcxzy");
        secondChain.beginConditionCountBlock("xzyabc");

        firstChain.setSpecialTransaction("abcxzy");
        secondChain.setSpecialTransaction("xzyabc");

        int invalidCount = 30;
        int count = 0;
        while (8 > firstChain.getConditionBlockCount() ||  8 > secondChain.getConditionBlockCount()){
            System.out.printf("[MultiChain] Wait the condition to be true with target 1: 8, 2 : 8 and real value: %s %s\n",
                    firstChain.getConditionBlockCount(), secondChain.getConditionBlockCount());
            ThreadHelper.safeSleepSecond(1);
            ++count;
            if (count == invalidCount){
                secondChain.setInvalidConditionCountBlock("abcxzy");
                System.out.println("send invalid blockchain");
            }
        }
        System.out.printf("[MultiChain] Condition matched to be true with target 1: 8, 2 : 8 and real value: %s %s\n",
                firstChain.getConditionBlockCount(), secondChain.getConditionBlockCount());
        firstChain.setStop();
        secondChain.setStop();
    }

    public void testMultiChainWithSpecialTxWithInvalidBlockInLight(){
        OneChain firstChain = new OneChain();
        OneChain secondChain = new OneChain();

        firstChain.setMiningMeanTime(2);
        secondChain.setMiningMeanTime(2);

        OneChainService firstChainService = new OneChainService();
        firstChainService.startService(firstChain, OneChain.getMiningTxNumber() * 3);

        OneChainService secondChainService = new OneChainService();
        secondChainService.startService(secondChain);

        ThreadHelper.safeSleepSecond(2);

        firstChain.beginConditionCountBlock("abcxzy");
        secondChain.beginConditionCountBlock("xzyabc");

        firstChain.setSpecialTransaction("abcxzy");
        secondChain.setSpecialTransaction("xzyabc");

        int invalidCount = 30;
        int count = 0;
        while (8 > firstChain.getConditionBlockCount() ||  8 > secondChain.getConditionBlockCount()){
            System.out.printf("[MultiChain] Wait the condition to be true with target 1: 8, 2 : 8 and real value: %s %s\n",
                    firstChain.getConditionBlockCount(), secondChain.getConditionBlockCount());
            ThreadHelper.safeSleepSecond(1);
            ++count;
            if (count == invalidCount){
                secondChain.setInvalidConditionCountBlock("abcxzy");
                System.out.println("send invlid blockchain");
            }
        }
        System.out.printf("[MultiChain] Condition matched to be true with target 1: 8, 2 : 8 and real value: %s %s\n",
                firstChain.getConditionBlockCount(), secondChain.getConditionBlockCount());
        firstChain.setStop();
        secondChain.setStop();

    }

    public void testMultiChainWithSpecialTxWithInvalidBlock(){
        OneChain firstChain = new OneChain();
        OneChain secondChain = new OneChain();

        firstChain.setMiningMeanTime(2);
        secondChain.setMiningMeanTime(2);

        OneChainService firstChainService = new OneChainService();
        firstChainService.startService(firstChain, OneChain.getMiningTxNumber() * 3);

        OneChainService secondChainService = new OneChainService();
        secondChainService.startService(secondChain);

        ThreadHelper.safeSleepSecond(2);

        firstChain.beginConditionCountBlock("abcxzy");
        secondChain.beginConditionCountBlock("xzyabc");

        firstChain.setSpecialTransaction("abcxzy");
        secondChain.setSpecialTransaction("xzyabc");

        int invalidCount = 30;
        int count = 0;
        while (8 > firstChain.getConditionBlockCount() ||  8 > secondChain.getConditionBlockCount()){
            System.out.printf("[MultiChain] Wait the condition to be true with target 1: 8, 2 : 8 and real value: %s %s\n",
                    firstChain.getConditionBlockCount(), secondChain.getConditionBlockCount());
            ThreadHelper.safeSleepSecond(1);
            ++count;
            if (count == invalidCount){
                firstChain.setInvalidConditionCountBlock("abcxzy");
                System.out.println("send invlid blockchain");
            }
        }
        System.out.printf("[MultiChain] Condition matched to be true with target 1: 8, 2 : 8 and real value: %s %s\n",
                firstChain.getConditionBlockCount(), secondChain.getConditionBlockCount());
        firstChain.setStop();
        secondChain.setStop();

    }

    public void testMultiChainWithSpecialTx(){
        OneChain firstChain = new OneChain();
        OneChain secondChain = new OneChain();

        firstChain.setMiningMeanTime(4);
        secondChain.setMiningMeanTime(4);

        OneChainService firstChainService = new OneChainService();
        firstChainService.startService(firstChain, OneChain.getMiningTxNumber() * 3);

        OneChainService secondChainService = new OneChainService();
        secondChainService.startService(secondChain);

        ThreadHelper.safeSleepSecond(2);

        firstChain.beginConditionCountBlock("abcxzy");
        secondChain.beginConditionCountBlock("xzyabc");

        firstChain.setSpecialTransaction("abcxzy");
        secondChain.setSpecialTransaction("xzyabc");

        while (8 > firstChain.getConditionBlockCount() ||  8 > secondChain.getConditionBlockCount()){
            System.out.printf("[MultiChain] Wait the condition to be true with target 1: 8, 2 : 8 and real value: %s %s\n",
                    firstChain.getConditionBlockCount(), secondChain.getConditionBlockCount());
            ThreadHelper.safeSleepSecond(1);
        }
        System.out.printf("[MultiChain] Condition matched to be true with target 1: 8, 2 : 8 and real value: %s %s\n",
                firstChain.getConditionBlockCount(), secondChain.getConditionBlockCount());
        firstChain.setStop();
        secondChain.setStop();

    }

    public void testMultiChain(){
        OneChain firstChain = new OneChain();
        OneChain secondChain = new OneChain();

        firstChain.setMiningMeanTime(8);
        secondChain.setMiningMeanTime(2);

        OneChainService firstChainService = new OneChainService();
        firstChainService.startService(firstChain);

        OneChainService secondChainService = new OneChainService();
        secondChainService.startService(secondChain);

        ThreadHelper.safeSleepSecond(2);

        firstChain.beginCountBlock();
        secondChain.beginCountBlock();
        while (8 > firstChain.getConditionBlockCount() ||  8 > secondChain.getConditionBlockCount()){
            System.out.printf("[MultiChain] Wait the condition to be true with target 1: 8, 2 : 8 and real value: %s %s\n",
                    firstChain.getConditionBlockCount(), secondChain.getConditionBlockCount());
            ThreadHelper.safeSleepSecond(1);
        }
        System.out.printf("[MultiChain] Condition matched to be true with target 1: 8, 2 : 8 and real value: %s %s\n",
                firstChain.getConditionBlockCount(), secondChain.getConditionBlockCount());
        firstChain.setStop();
        secondChain.setStop();

    }

    public void testSameThreeChain(){
        OneChain firstChain = new OneChain();
        OneChain secondChain = new OneChain();
        OneChain thirdChain = new OneChain();

        firstChain.setMiningMeanTime(4);
        secondChain.setMiningMeanTime(4);
        thirdChain.setMiningMeanTime(4);

        OneChainService firstChainService = new OneChainService();
        firstChainService.startService(firstChain);

        OneChainService secondChainService = new OneChainService();
        secondChainService.startService(secondChain);

        OneChainService thirdChainService = new OneChainService();
        secondChainService.startService(thirdChain);

        ThreadHelper.safeSleepSecond(2);

        firstChain.beginCountBlock();
        secondChain.beginCountBlock();
        while (8 > firstChain.getConditionBlockCount() ||  8 > secondChain.getConditionBlockCount() ||  8 > thirdChain.getConditionBlockCount()){
            System.out.printf("[MultiChain] Wait the condition to be true,                                         current value: %s   %s   %s\n",
                    firstChain.getConditionBlockCount(), secondChain.getConditionBlockCount(), thirdChain.getConditionBlockCount());
            ThreadHelper.safeSleepSecond(1);
        }
        System.out.printf("[MultiChain] Condition matched to be true,                                          current value: %s   %s   %s\n",
                firstChain.getConditionBlockCount(), secondChain.getConditionBlockCount(), thirdChain.getConditionBlockCount());
        firstChain.setStop();
        secondChain.setStop();

    }

    public void testThreeChain(){
        OneChain firstChain = new OneChain();
        OneChain secondChain = new OneChain();
        OneChain thirdChain = new OneChain();

        firstChain.setMiningMeanTime(8);
        secondChain.setMiningMeanTime(4);
        thirdChain.setMiningMeanTime(2);

        OneChainService firstChainService = new OneChainService();
        firstChainService.startService(firstChain);

        OneChainService secondChainService = new OneChainService();
        secondChainService.startService(secondChain);

        OneChainService thirdChainService = new OneChainService();
        secondChainService.startService(thirdChain);

        ThreadHelper.safeSleepSecond(2);

        firstChain.beginCountBlock();
        secondChain.beginCountBlock();
        while (8 > firstChain.getConditionBlockCount() ||  8 > secondChain.getConditionBlockCount() ||  8 > thirdChain.getConditionBlockCount()){
            System.out.printf("[MultiChain] Wait the condition to be true,                                         current value: %s   %s   %s\n",
                    firstChain.getConditionBlockCount(), secondChain.getConditionBlockCount(), thirdChain.getConditionBlockCount());
            ThreadHelper.safeSleepSecond(1);
        }
        System.out.printf("[MultiChain] Condition matched to be true,                                          current value: %s   %s   %s\n",
                firstChain.getConditionBlockCount(), secondChain.getConditionBlockCount(), thirdChain.getConditionBlockCount());
        firstChain.setStop();
        secondChain.setStop();

    }
}