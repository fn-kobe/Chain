package util;

import org.junit.Test;

import static org.junit.Assert.*;

public class GasHandlerTest {

	@Test
	public void processGas() {
		GasHandler gasHandler = new GasHandler();
		assert !gasHandler.processGas(5);
	}
}