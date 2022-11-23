package asset;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import util.DESHelper;
import util.SftpHelper;

// 6 + 2 + 1
public class Big_data extends Data {
    final String asset_type = "big_data";
    String size = "";

    String serverIp = "";
    String userName = "";
    String password = "";
    String fileName = "";

    //String data; // Not used as we directly return the URL

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public Big_data(String name, String ownerName, String keyword, String hash) {
        super(name, ownerName, keyword, hash);
        setType(asset_type);
    }

    public Big_data(Big_data big_data) {
        super(big_data.name, big_data.ownerName, big_data.keyword, big_data.hash);
        setType(big_data.asset_type);
        size = big_data.size;
        serverIp = big_data.serverIp;
        userName = big_data.userName;
        password = big_data.password;
        fileName = big_data.fileName;
    }

    // transfer
    public boolean transfer(String typeName, String buyerName, String buyerKey) {
        if (serverIp.isEmpty() || userName.isEmpty() || password.isEmpty() || fileName.isEmpty()){
            System.out.println("Some transfer argument is missing: " + serverIp + " : " + userName + " : " +  password + " : " + fileName);
            return false;
        }
        return SftpHelper.getFile(serverIp,userName, password, fileName);
    }

    // transfer
    public String urlTransfer(String typeName, String buyerName, String buyerKey) {
        System.out.println("Begin to transfer the asset");
        if (typeName != type) {
            System.out.println("Skip asset transfer, type mismatched, MultiTypeAsset type: " + type + " : (request type)" + typeName);
            return "";
        }

        // should create one share url, like FTP or similar
        String outputUrl = serverIp + " : " + userName + " : " +  password + " : " + fileName;
        try {
            outputUrl = DESHelper.encryptBase64(buyerKey, outputUrl);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            System.out.println("Failed to encrypt the data");
            System.out.println("Failed to transfer the asset");
            return "";
        }
        System.out.println("Succeed to transfer the asset");// We ignore the transfer process
        return outputUrl;
    }

    public boolean isMatched(Big_data requiredAsset) {
        if (!super.isMatched(requiredAsset)) return false;

        if (requiredAsset.getSize().isEmpty()) return true;

        int requiredSize = 0;
        int selfSize = 0;
        try {
            requiredSize = Integer.parseInt(requiredAsset.getSize());
            selfSize = Integer.parseInt(size);
        } catch (NumberFormatException e) {
            System.out.println("Size format error: " + requiredAsset.getSize() + " : (self sze)" + size);
            return false;
        }
        if (selfSize < requiredSize) return false;

        return true;
    }

    public JSONObject getJson() {
        JSONObject object = super.getJson();
        object.put("size", size);
        object.put("serverIp", serverIp);
        object.put("userName", userName);
        object.put("password", password);
        object.put("fileName", fileName);
        return object;
    }

    @NotNull
    public static Big_data createBig_data(JSONObject object) {
        String name = safeGetJsonStringValue(object, "name");
        String ownerName = safeGetJsonStringValue(object, "ownerName");
        String priceUnit = safeGetJsonStringValue(object, "priceUnit");
        String hash = safeGetJsonStringValue(object, "hash");
        int price = safeGetJsonIntValue(object, "price");
        String keyword = safeGetJsonStringValue(object, "keyword");
        String size = safeGetJsonStringValue(object, "size");
        String serverIp = safeGetJsonStringValue(object, "serverIp");
        String userName = safeGetJsonStringValue(object, "userName");
        String password = safeGetJsonStringValue(object, "password");
        String fileName = safeGetJsonStringValue(object, "fileName");
        Big_data asset = new Big_data(name, ownerName, keyword, hash);
        asset.setPrice(price);
        asset.setPriceUnit(priceUnit);
        asset.setSize(size);
        asset.setServerIp(serverIp);
        asset.setUserName(userName);
        asset.setPassword(password);
        asset.setFileName(fileName);
        return asset;
    }

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
