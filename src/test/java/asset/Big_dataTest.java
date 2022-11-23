package asset;

import junit.framework.TestCase;

public class Big_dataTest extends TestCase {

    public void testUrlTransfer() {
        Big_data big_data = new Big_data("City traffic data", "CDU", "traffic", "00EF123E");
        String dataUrl = big_data.urlTransfer("big_data", "Gui Yang", "1234567890");
        assert !dataUrl.isEmpty();
    }

    public void testTransfer() {
/*        Big_data big_data = new Big_data("City traffic data", "CDU", "traffic", "00EF123E");
        big_data.setServerIp("192.168.30.131");
        big_data.setUserName("suhong");
        big_data.setPassword("scu123");
        big_data.setFileName("asset.js");
        assert  big_data.transfer("big_data", "Gui Yang", "1234567890")*/;
    }

    public void testIsMatched() {
        Big_data requiredData = new Big_data("City traffic data", "CDU", "traffic", "00EF123E");
        Big_data publishedData = new Big_data("City traffic data", "CDU", "traffic", "00EF123E");
        assert publishedData.isMatched(requiredData);

        requiredData.setSize("5");
        assert !publishedData.isMatched(requiredData);

        publishedData.setSize("6");
        assert publishedData.isMatched(requiredData);
    }

    public void testToJson(){
        Big_data testData = new Big_data("City traffic data", "CDU", "traffic", "00EF123E");
        System.out.println(testData.getJson().toString());
    }
}