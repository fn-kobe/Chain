package asset;

import Service.BlockchainService;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// 6 fields
public class Asset {
    // Asset self character
    String name ="";
    String type = "asset"; // this is controlled by Asset itself

    // Asset trade parameters
    String priceUnit = "DM";// digital monetary
    int price = 0;

    // transfer definitions
    String transferMethod = "default"; // default is directly get the Asset

    String ownerName = "";

    //

    BlockchainService blockchainService = null;
    public Asset(String name, String ownerName) {
        this.name = name;
        this.ownerName = ownerName;
        blockchainService = BlockchainService.getInstance();
    }

    // publish the Asset

    public boolean publish(){
        System.out.println("Begin to publish asset");
        boolean r = safeSendMessage(getDescription());
        if(!r){
            System.out.println("Failed ro publish asset");
        } else {
            System.out.println("Succeed to publish asset");
        }
        return r;
    }

    public JSONObject getJson() {
        JSONObject object = new JSONObject();
        object.put("name", name);
        object.put("ownerName", ownerName);
        object.put("price", price);
        object.put("type", type);
        object.put("priceUnit", priceUnit);
        object.put("transferMethod", transferMethod);
        return object;
    }

    @NotNull
    public static Asset createBig_data(JSONObject object) {
        String name = safeGetJsonStringValue(object, "name");
        String ownerName = safeGetJsonStringValue(object, "ownerName");
        String priceUnit = safeGetJsonStringValue(object, "priceUnit");
        int price = safeGetJsonIntValue(object, "price");
        Asset asset = new Asset(name, ownerName);
        asset.setPrice(price);
        asset.setPriceUnit(priceUnit);
        return asset;
    }

    public String getDescription() {
        return getJson().toString();
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    // negotiation
    public boolean negotiation(int price){
        System.out.println("Begin to negotiate the asset price");
        boolean r =  price >= this.price;
        if (!r){
            System.out.println("Failed to negotiate the asset price");
        } else {
            System.out.println("Succeed to negotiate the asset price");
        }
        return r;
    }

    // transfer
    public boolean transfer(String typeName, String buyerName){
        System.out.println("Begin to transfer the asset");
        if (typeName != type){
            System.out.println("Skip asset transfer, type mismatched, Asset type: " + type + " : (request type)" + typeName);
            return false;
        }
        if (buyerName.isEmpty()){
            System.out.println("No buyer name set");
            return false;
        }
        //simplest type, just record in blockchain
        // TO DO add logic for Asset check, we ignore here
        String transferMsg = getDescription();
        transferMsg += buyerName;
        safeSendMessage(transferMsg);
        System.out.println("Succeed to transfer the asset");
        return true;
    }

    private boolean safeSendMessage(String msg) {
        try {
            List<String> arguments = new ArrayList<>();
            arguments.add(msg);
            arguments.add("0xabc");
            arguments.add("0xabd");
            arguments.add("12");
            blockchainService.triggerTransaction(arguments);
            System.out.println("Succeed to send asset message to the block chain: " + msg);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to send asset message to the block chain: " + msg);
        }
        return false;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public String getPriceUnit() {
        return priceUnit;
    }

    public void setPriceUnit(String priceUnit) {
        this.priceUnit = priceUnit;
    }

    public String getTransferMethod() {
        return transferMethod;
    }

    public void setTransferMethod(String transferMethod) {
        this.transferMethod = transferMethod;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public boolean isMatched(Asset requiredAsset){
        if (!requiredAsset.getName().isEmpty() && !requiredAsset.getName().equals(name)) return false;

        if (!requiredAsset.getPriceUnit().isEmpty() && !requiredAsset.getPriceUnit().equals(priceUnit)) return false;

        if (!requiredAsset.getOwnerName().isEmpty() && !requiredAsset.getOwnerName().contains(ownerName)) return false;

        if (requiredAsset.getPrice() != 0 && requiredAsset.getPrice() >=  price) return false;

        return true;
    }

    void setType(String type){
        this.type = type;
    }

    static String safeGetJsonStringValue(JSONObject object, String key) {
        String r = "";
        try {
            r = (String) object.get(key);
        }catch (JSONException e){
            System.out.println("[AssetService] Can not get value of " + key);
        }
        return r;
    }

    static int safeGetJsonIntValue(JSONObject object, String key) {
        int r = 0;
        try {
            r = (int) object.get(key);
        }catch (JSONException e){
            System.out.println("[AssetService] Can not get value of " + key);
        }
        return r;
    }
}
