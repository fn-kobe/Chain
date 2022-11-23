package util;

public class GasHandler {
	private long givenGas = 0L;
	private long leftGas = 0L;

	// Currently, we only calculate the gas by time
	public boolean processGas(long startTime, long endTime) {
		return processGas(calculateGasByTime(startTime, endTime));

	}

	public boolean processGas(long requiredGas) {
		System.out.println("[GasHandler][Info] Gas required is " + requiredGas);

		if (this.leftGas - requiredGas < 0L) {
			leftGas = 0;
			System.out.printf("[GasHandler][ERROR] Not enough gas. Left gas:%s, required gas:%s \n", leftGas, requiredGas);
			return false;
		}
		leftGas -= requiredGas;
		return true;
	}

	public static long calculateGasByTime(long startTime, long endTime){
		return calculateGasByTime(endTime-startTime);
	}

	public static long calculateGasByTime(long totalTime){
		return totalTime; // we only map 1 to 1 and will have more in future if needed
	}

	public boolean hasGas(){
		return leftGas > 0;
	}

	public void setGivenGas(long givenGas) {
		this.givenGas = givenGas;
		this.leftGas = givenGas;
	}

	public long getTotalUsedGas() {
		return givenGas - leftGas;
	}
}
