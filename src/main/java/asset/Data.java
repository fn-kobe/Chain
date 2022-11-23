package asset;

import org.json.JSONObject;

// 6 + 2 fields
public class Data extends MultiTypeAsset {
    String keyword = "basic_asset";
    final String asset_type = "data";
    String hash = "";


    public Data(String name, String ownerName, String keyword, String hash) {
        super(name, ownerName);
        setType(asset_type);
        this.keyword = keyword;
        this.hash = hash;
    }

    public String getKeyWord(){
        return keyword;
    }

    public boolean isMatched(Data requiredAsset){
        if (!super.isMatched(requiredAsset)) return false;

        if (!requiredAsset.getKeyWord().isEmpty() && !keyword.contains(requiredAsset.getKeyWord())) return false;

        return true;
    }

    public JSONObject getJson() {
        JSONObject object = super.getJson();
        object.put("keyword", keyword);
        object.put("hash", hash);
        return object;
    }

    public static MultiTypeAsset createData(JSONObject object) {
        String name = safeGetJsonStringValue(object, "name");
        String ownerName = safeGetJsonStringValue(object, "ownerName");
        String priceUnit = safeGetJsonStringValue(object, "priceUnit");
        String hash = safeGetJsonStringValue(object, "hash");
        int price = safeGetJsonIntValue(object, "price");

        String keyword = safeGetJsonStringValue(object, "keyword");
        Data asset = new Data(name, ownerName, keyword, hash);
        asset.setPrice(price);
        asset.setPriceUnit(priceUnit);
        return asset;
    }
}
