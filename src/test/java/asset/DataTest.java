package asset;

import junit.framework.TestCase;

import java.util.Date;

public class DataTest extends TestCase {

    public void testIsMatched() {
        Data publishedData = new Data("data1", "Henry", "block chain application", "00EF123E");
        Data requiredData = new Data("data1", "Henry", "block", "00EF123E");
        assert publishedData.isMatched(requiredData);

        requiredData = new Data("data1", "Henry", "blocks", "00EF123E");
        assert !publishedData.isMatched(requiredData);

        requiredData = new Data("", "Henry", "", "00EF123E");
        assert publishedData.isMatched(requiredData);
    }

    public void testToJson(){
        Data testData = new Data("data1", "Henry", "block chain application", "00EF123E");
        System.out.println(testData.getJson().toString());
    }
}