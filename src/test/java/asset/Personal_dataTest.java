package asset;

import junit.framework.TestCase;
import org.json.JSONObject;

public class Personal_dataTest extends TestCase {

    public void testIsMatched() {
        Personal_data publishedData = new Personal_data("Person 1", "Henry", "ZhangSan chengdu", "00EF123E");
        Personal_data requiredData = new Personal_data("Person 1", "Henry", "ZhangSan chengdu", "00EF123E");
        assert publishedData.isMatched(requiredData);

        requiredData = new Personal_data("", "Henry", "ZhangSan chengdu", "00EF123E");
        assert publishedData.isMatched(requiredData);

        requiredData = new Personal_data("", "Henry", "ZhangSan chengdu", "00EF123E");
        requiredData.setPersonal_name("Zhang san");
        assert !publishedData.isMatched(requiredData);

        requiredData = new Personal_data("", "Henry", "ZhangSan chengdu", "00EF123E");
        publishedData.setPersonal_name("Zhang san");
        assert publishedData.isMatched(requiredData);

        requiredData = new Personal_data("", "Henry", "ZhangSan chengdu", "00EF123E");
        requiredData.setAddress("Chengdu");
        assert !publishedData.isMatched(requiredData);

        requiredData = new Personal_data("", "Henry", "ZhangSan chengdu", "00EF123E");
        publishedData.setPersonal_name("Chengdu");
        assert publishedData.isMatched(requiredData);

        requiredData = new Personal_data("", "Henry", "ZhangSan chengdu", "00EF123E");
        requiredData.setPhoneNumber("13612345678");
        assert !publishedData.isMatched(requiredData);

        requiredData = new Personal_data("", "Henry", "ZhangSan chengdu", "00EF123E");
        publishedData.setPhoneNumber("13612345678");
        assert publishedData.isMatched(requiredData);
    }

    public void testGetJson() {
        Personal_data requiredData = new Personal_data("Person 1", "Henry", "ZhangSan chengdu", "00EF123E");
        JSONObject object = requiredData.getJson();
        System.out.println(object.toString());
        assert object.get("name").equals("Person 1");
        assert object.get("ownerName").equals("Henry");
        assert object.get("keyword").toString().contains("chengdu");
    }
}