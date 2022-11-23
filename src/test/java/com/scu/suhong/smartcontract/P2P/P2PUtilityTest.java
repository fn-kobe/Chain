package com.scu.suhong.smartcontract.P2P;

import org.junit.Test;
import util.RandomHelper;

import java.util.ArrayList;
import java.util.List;

public class P2PUtilityTest {

	@Test
	public void matchingMethodCompare() {
		int[] totalNumberArray = {32, 64, 128, 512, 1024};
		int[] groupElementNumberArray = {2, 4, 8, 16};
		for (int i = 0; i < totalNumberArray.length; ++i) {
			int totalNumber = totalNumberArray[i];
			for (int j = 0; j < groupElementNumberArray.length; ++j) {
				int groupCount = totalNumber / groupElementNumberArray[j];
				simulateComparisonCount(totalNumber, groupCount);
			}
		}
	}


	// Simulate the waiting time by a random long waiting time
	@Test
	public void simulateExchangeTime() {
		int testRound = 1132;
		doSimulateExchangeTime(testRound);
	}

	void doSimulateExchangeTime(int repeatTime) {
		int userNumber = 16;
		int[] groupSizeArray = {2, 4, 8};
		for (int i = 0; i < groupSizeArray.length; ++i) {
			for (int j = 0; j < repeatTime; ++j) {
				doSimulateExchangeTime(userNumber, groupSizeArray[i]);
			}
		}
	}

	void doSimulateExchangeTime(int userNumber, int groupSize) {
		int longTime = 100;

		//1. generate time
		RandomHelper randomHelper = new RandomHelper();
		int longTimeUser = randomHelper.getNumber(userNumber);
		int[] userTime = new int[userNumber];
		userTime[longTimeUser] = longTime;

		for (int i = 0; i < userNumber; ++i) {
			if (i != longTimeUser) userTime[i] = randomHelper.getNumber(10) + 1;// 1 to 10 seconds
		}

		//.2 calculate
		int tmpGroupSum = 0;
		int totalSum = 0;
		int previousId = 0;
		for (int i = 0; i < userNumber; ++i) {
			int numberInOneGroup = i % groupSize + 1;
			if (1 == numberInOneGroup && 0 != i) {//first member and then a new group start
				// a, process previous
				int previousAverageTime = tmpGroupSum / groupSize;
				System.out.printf("[TEST][INFO] Group %d average waiting time %d\n", previousId, previousAverageTime);
				//b, initialization for new
				tmpGroupSum = userTime[i];
				totalSum += userTime[i];
				++previousId;
			} else {
				tmpGroupSum += userTime[i] * numberInOneGroup;// all member must wait together
				totalSum += userTime[i] * numberInOneGroup;
			}
		}
		// last group
		int lastGroupAverageTime = tmpGroupSum / groupSize;
		System.out.printf("[TEST][INFO] Group %d average waiting time %d\n", previousId, lastGroupAverageTime);
		System.out.printf("[TEST][INFO] All group average waiting time %d with %d groups\n", totalSum / userNumber, previousId + 1);
	}

	// test the different comparison times by different groups
	@Test
	public void matchingMethodCompareByNumber() {
		int testRound = 32;
		doMatchingMethodCompareByNumber(testRound);
	}

	public void doMatchingMethodCompareByNumber(int repeatCount) {
		int[] totalNumberArray = {32, 64, 128, 512, 1024};
		int[] groupElementNumberArray = {2, 4, 8, 16};
		for (int i = 0; i < totalNumberArray.length; ++i) {
			int totalNumber = totalNumberArray[i];
			int elementInOneGroup = 3;// 1-2, 2-4, 3-8, 4 -16 element in one group
			int groupCount = totalNumber / groupElementNumberArray[elementInOneGroup];
			repeatSimulateComparisonCount(totalNumber, groupCount, repeatCount);
		}
	}

	@Test
	public void matchingMethodCompareByGroupNuber() {
		int testRound = 32;
		doMatchingMethodCompareByGroupNuber(testRound);
	}

	public void doMatchingMethodCompareByGroupNuber(int repeatCount) {
		int[] totalNumberArray = {32, 64, 128, 512, 1024};
		int[] groupElementNumberArray = {2, 4, 8, 16, 32, 64, 128, 256, 512};
		int i = 3;//3-512, (32, 64, 128, 512, 1024)
		int totalNumber = totalNumberArray[i];//
		for (int j = 0; j < groupElementNumberArray.length; ++j) {
			int groupCount = totalNumber / groupElementNumberArray[j];
			repeatSimulateComparisonCount(totalNumber, groupCount, repeatCount);
		}
	}

	void repeatSimulateComparisonCount(int totalNumber, int groupCount, int repeatCount) {
		for (int i = 0; i < repeatCount; ++i) {
			simulateComparisonCount(totalNumber, groupCount);
		}
	}

	int simulateComparisonCount(int totalNumber, int groupCount) {
		int targetSCNumber = generateTargetSCNumber(totalNumber);
		int pairedSCNumber = generatePairedSCNumber(totalNumber);
		while (targetSCNumber == pairedSCNumber)
			pairedSCNumber = generatePairedSCNumber(totalNumber);// make target and paired SC not the same
		List<List<Integer>> groupList = generateEmptyGroupList(groupCount);
		generateSCFilledGroup(targetSCNumber, groupCount, groupList);
		int pairedSCGroupIndex = getPairedSCGroupIndex(pairedSCNumber, groupList);

		int comparisonCount = 0;
		// all element will be compared, as the paired element is still not generated
		// as it generated later than the target SC
		if (-1 == pairedSCGroupIndex) {
			// as the target and the paired sc are in the same group as the target does not appear
			// We just compare in the target group.
			// In fact group should be sure among users
			int targetSCGroupIndex = getTargetSCGroupIndex(targetSCNumber, groupList);
			System.out.println("[TEST][INFO] Paired SC is in group same as the target " + targetSCGroupIndex);
			// -1 is not to compare with itself, target, +1 is to use a map to find corresponding group
			comparisonCount = groupList.get(targetSCGroupIndex).size() - 1 + 1;
		} else {
			System.out.println("[TEST][INFO] Paired SC is in group " + pairedSCGroupIndex);
			// +1 is to use a map to find corresponding group
			comparisonCount = findPairedSC(pairedSCNumber, pairedSCGroupIndex, groupList) + 1;
		}

		System.out.printf("[TEST][INFO] Comparison count is %d for the test with total number %d and group count %d, target %d, paired %d\n",
						comparisonCount, totalNumber, groupCount, targetSCNumber, pairedSCNumber);

		//System.out.printf("[TEST][INFO] Comparison count is %d for all matching, target %d, paired %d\n",
		//				(targetSCNumber > pairedSCNumber) ? (targetSCNumber - 1) : pairedSCNumber, targetSCNumber, pairedSCNumber);// plus 1 is to remove compare with itself
		return comparisonCount;
	}

	int findPairedSC(int pairedSCNumber, int pairedGroupIndex, List<List<Integer>> groupList) {
		List<Integer> group = groupList.get(pairedGroupIndex);
		for (int i = 0; i < group.size(); ++i) {
			if (group.get(i) == pairedSCNumber) return i + 1;
		}
		assert false;// paired group does not contains the paired element
		return -1;
	}

	int generateTargetSCNumber(int maxNumber) {
		RandomHelper randomHelper = new RandomHelper(maxNumber);
		return randomHelper.getNumber() + 1;
	}

	int generatePairedSCNumber(int maxNumber) {
		RandomHelper randomHelper = new RandomHelper(maxNumber);
		return randomHelper.getNumber() + 1;
	}

	List<List<Integer>> generateEmptyGroupList(int groupCount) {
		List<List<Integer>> r = new ArrayList<>();
		for (int i = 0; i < groupCount; ++i) {
			r.add(new ArrayList<>());
		}
		return r;
	}

	void generateSCFilledGroup(int targetNumber, int arrayCount, List<List<Integer>> groupList) {
		assert groupList.size() == arrayCount;

		RandomHelper randomHelper = new RandomHelper(arrayCount);
		for (int i = 0; i < targetNumber; ++i) {
			int selectArrayIndex = randomHelper.getNumber();
			groupList.get(selectArrayIndex).add(i + 1);
		}

		//debug and output generate information
		outPutSCGroup(groupList);
	}

	void outPutSCGroup(List<List<Integer>> groupList) {
		System.out.println("[TEST][DEBUG] Begin to dump generated group list.");
		for (int i = 0; i < groupList.size(); ++i) {
			String r = "[" + i + "]";
			List<Integer> group = groupList.get(i);
			for (int j = 0; j < group.size(); ++j) {
				r += " " + group.get(j);
			}
			System.out.println(r);
		}
		System.out.println("[TEST][DEBUG] End to dump.");
	}

	// return pairedSC index if the sequence before target sc contains this element
	int getPairedSCGroupIndex(int pairedSCNumber, List<List<Integer>> groupList) {
		return getSCIndexInGroup(pairedSCNumber, groupList);
	}

	// return targetSC index if the sequence before target sc contains this element
	int getTargetSCGroupIndex(int targetSCNumber, List<List<Integer>> groupList) {
		return getSCIndexInGroup(targetSCNumber, groupList);
	}

	int getSCIndexInGroup(int sCNumber, List<List<Integer>> groupList) {
		for (int i = 0; i < groupList.size(); ++i) {
			List<Integer> tmpList = groupList.get(i);
			for (int j = 0; j < tmpList.size(); ++j) {
				if (sCNumber == tmpList.get(j)) {
					return i;
				}
			}
		}
		return -1;
	}

}