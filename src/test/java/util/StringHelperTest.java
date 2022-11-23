package util;

import junit.framework.TestCase;

import java.util.List;

public class StringHelperTest extends TestCase {

    public void testSplitCommandString() {
        List<String> testList = StringHelper.splitCommandString("This is  a car");
        assert(4 == testList.size());
        assert(testList.get(0).equals("This"));
    }

    public void testGetIPv4String() {
        String ipV4 = StringHelper.getIPv4String("1.1.1.1w");
        assert (StringHelper.isValidIP(ipV4));
        ipV4 = StringHelper.getIPv4String("1.1.1.1");
        assert (StringHelper.isValidIP(ipV4));
    }

    public void testGenerateRepeatCharString() {
        String s = StringHelper.generateRandomString('a', 30, 3);
        System.out.println(s);

        s = StringHelper.generateRandomString('a', 30, 3);
        System.out.println(s);
    }

    public void testGenerateRandomChar() {
        char gc = StringHelper.generateRandomChar();
        assert gc >= 'a' && gc <= 'z';
    }

		public void testGetValueFromKeyValueParameters() {
			String keyValueParameterString = "c#code,n#name,i#instance";
			assert "name".equals(StringHelper.getValueFromKeyValueParameters(
							"n",keyValueParameterString, ",", "#"));
		}
}