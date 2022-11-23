package asset;

import org.json.JSONObject;

// 6 + 2 + 3
public class Personal_data extends Data {
    static String asset_type = "personal_data";

    String personal_name = "";
    String phoneNumber = "";
    String address = "";

    public Personal_data(String name, String ownerName,String keyword, String hash) {
        super(name, ownerName, keyword, hash);
        setType(asset_type);
    }

    public Personal_data(Personal_data personal_data) {
        super(personal_data.name, personal_data.ownerName, personal_data.keyword, personal_data.hash);
        setType(asset_type);
    }

    public String getPersonal_name() {
        return personal_name;
    }

    public void setPersonal_name(String personal_name) {
        this.personal_name = personal_name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isMatched(Personal_data requiredAsset){
        if (!super.isMatched(requiredAsset)) return false;

        if (!requiredAsset.getPersonal_name().isEmpty() && !requiredAsset.getPersonal_name().equals(getPersonal_name())) return false;

        if (!requiredAsset.getPhoneNumber().isEmpty() && !requiredAsset.getPhoneNumber().equals(getPhoneNumber())) return false;

        if (!requiredAsset.getAddress().isEmpty() && !requiredAsset.getAddress().equals(getAddress())) return false;

        return true;
    }

    public JSONObject getJson() {
        JSONObject object = super.getJson();
        object.put("personal_name", personal_name);
        object.put("phoneNumber", phoneNumber);
        object.put("address", address);
        return object;
    }

    public static MultiTypeAsset createPersonal_data(JSONObject object) {
        String name = safeGetJsonStringValue(object, "name");
        String ownerName = safeGetJsonStringValue(object, "ownerName");
        String priceUnit = safeGetJsonStringValue(object, "priceUnit");
        String hash = safeGetJsonStringValue(object, "hash");
        int price = safeGetJsonIntValue(object, "price");
        String keyword = safeGetJsonStringValue(object, "keyword");
        String personal_name = safeGetJsonStringValue(object, "personal_name");
        String phoneNumber = safeGetJsonStringValue(object, "phoneNumber");
        String address = safeGetJsonStringValue(object, "address");
        Personal_data asset = new Personal_data(name, ownerName, keyword, hash);
        asset.setPrice(price);
        asset.setPriceUnit(priceUnit);
        asset.setPersonal_name(personal_name);
        asset.setPhoneNumber(phoneNumber);
        asset.setAddress(address);
        return asset;
    }
}
