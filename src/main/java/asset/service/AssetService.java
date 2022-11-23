package asset.service;

import Service.BlockchainService;
import asset.MultiTypeAsset;
import asset.Big_data;
import asset.Data;
import asset.Personal_data;
import com.scu.suhong.block.Block;
import com.scu.suhong.block.BlockChain;
import com.scu.suhong.dynamic_definition.AbstractTransaction;
import com.scu.suhong.transaction.Transaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;
import util.FileHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AssetService {
    static String toBeFoundAsset = "toBeDiscoverAsset"; // user can put the asset requirement here in JSON format
    static String toBePublishAsset = "toBePublishAsset"; // user can put the asset requirement here in JSON format
    BlockchainService blockchainService = null;
    static String buyer = "";
    static String buyerKey = "";


    public static String getBuyer() {
        return buyer;
    }

    public static void setBuyer(String buyer) {
        AssetService.buyer = buyer;
    }

    public static String getBuyerKey() {
        return buyerKey;
    }

    public static void setBuyerKey(String buyerKey) {
        AssetService.buyerKey = buyerKey;
    }

    public boolean tradeAsset(){
        MultiTypeAsset multiTypeAsset = discoveryAsset();
        if (null == multiTypeAsset){
            return  false;
        }

        if (!multiTypeAsset.negotiation(multiTypeAsset.getPrice() + 0)){ // we only negotiate the same price
            return false;
        }

        return multiTypeAsset.transfer(multiTypeAsset.getType(), buyer);
    }

    public AssetPublishResult publishAsset()
    {
        MultiTypeAsset multiTypeAssetToPublish = loadOneAssetToPublish();
        if (null == multiTypeAssetToPublish){
            return AssetPublishResult.ENoAsset;
        }
        return publishAsset(multiTypeAssetToPublish);
    }

    public AssetPublishResult publishAsset(MultiTypeAsset multiTypeAssetToPublish)
    {
        blockchainService = BlockchainService.getInstance();
        try {
            List<String> arguments = new ArrayList<>();
            arguments.add(multiTypeAssetToPublish.getJson().toString());
            arguments.add("0xabc");
            arguments.add("0xabd");
            arguments.add("12");
            arguments.add(multiTypeAssetToPublish.getType());
            blockchainService.triggerTransaction(arguments);
            System.out.println("[AssetService] Succeed to publish asset ");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("[AssetService] Failed to send transaction " + multiTypeAssetToPublish.getJson().toString());
            return AssetPublishResult.EError;
        }
        return AssetPublishResult.EOK;
    }

    public MultiTypeAsset discoveryAsset() {
        MultiTypeAsset requiredMultiTypeAsset = loadOneRequiredAsset();
        return discoveryAsset(requiredMultiTypeAsset);
    }


    public MultiTypeAsset discoveryAsset(MultiTypeAsset requiredMultiTypeAsset) {
        if (null == requiredMultiTypeAsset) return null;
        BlockChain blockChain = BlockChain.getInstance();
        List<Block> blockList = blockChain.getBlockList();
        for (Block block: blockList) {
            for (AbstractTransaction t : block.getTransactions())
            {
                MultiTypeAsset publishedMultiTypeAsset = createFromJson(t.getData());
                if ( null == publishedMultiTypeAsset){
                    continue;// check next asset
                }
                if (isMatched(publishedMultiTypeAsset, requiredMultiTypeAsset)){
                    System.out.println("[AssetService] publishedMultiTypeAsset and requiredMultiTypeAsset matched");
                    return publishedMultiTypeAsset;
                } else {
                    System.out.println("[AssetService] publishedMultiTypeAsset and requiredMultiTypeAsset mismatched");
                }
            }
        }
        return null;
    }

    private boolean isMatched(MultiTypeAsset publishedMultiTypeAsset, MultiTypeAsset requiredMultiTypeAsset) {
        if (!publishedMultiTypeAsset.getType().equals(requiredMultiTypeAsset.getType())){
            System.out.println("[AssetService] public asset type is not matched");
            return false;
        }

        return typeMatch(publishedMultiTypeAsset, requiredMultiTypeAsset);
    }

    public static String getToBeDiscoverAssetFolderName() {
        return toBeFoundAsset;
    }

    public static void setToBeFoundAssetFolderName(String toBeFoundAsset) {
        AssetService.toBeFoundAsset = toBeFoundAsset;
    }


    public static String getToBePublishAssetFolderName() {
        return toBePublishAsset;
    }

    public static void setToBePublishAssetFolderName(String toBePublishAsset) {
        AssetService.toBePublishAsset = toBePublishAsset;
    }

    private boolean typeMatch(MultiTypeAsset publishedMultiTypeAsset, MultiTypeAsset requiredMultiTypeAsset) {
        String type = requiredMultiTypeAsset.getType();
        return publishedMultiTypeAsset.isMatched(requiredMultiTypeAsset);
    }

    public static MultiTypeAsset createFromJson(String data) {
        MultiTypeAsset multiTypeAsset = null;
        try {
            multiTypeAsset = createAsset(new JSONObject(data));
        } catch (JSONException e){
            System.out.println("[AssetService]  Invalid multiTypeAsset JSON format: " + data);
        }

        return multiTypeAsset;
    }

    @NotNull
    private static MultiTypeAsset createAsset(JSONObject object) {
        String type = safeGetJsonStringValue(object, "type");

        System.out.println("[AssetService] Try to create asset of type: " + type);
        if (type.equals("asset")) {
            return Data.createData(object);
        } else if (type.equals("data")) {
            return Data.createData(object);
        } else if (type.equals("big_data")) {
            return Big_data.createBig_data(object);
        } else if (type.equals("personal_data")) {
            return Personal_data.createPersonal_data(object);
        }

        System.out.println("[AssetService] [ERROR] Not support asset type: " + type);
        return null;
    }

    private static void transfer(MultiTypeAsset multiTypeAsset, String buyer, String buyerKey) {
        System.out.println("[AssetService] Try to transfer the multiTypeAsset: " + multiTypeAsset.getType()+ " with name: " + multiTypeAsset.getName());
        String type = multiTypeAsset.getType();
        if (type.equals("asset")) {
            multiTypeAsset.transfer("asset", buyer);
        } else if (type.equals("data")) {
            Data data =(Data) multiTypeAsset;
            data.transfer("data", buyer);
        } else if (type.equals("big_data")) {
            Big_data big_data = (Big_data) multiTypeAsset;
            big_data.transfer("big_data",buyer , buyerKey);
        } else if (type.equals("personal_data")) {
            Personal_data personal_data = (Personal_data) multiTypeAsset;
            personal_data.transfer("personal_data", buyer);
        }

        System.out.println("[AssetService] Succeed to transfer multiTypeAsset");
    }


    private static String safeGetJsonStringValue(JSONObject object, String key) {
        String r = "";
        try {
            r = (String) object.get(key);
        }catch (JSONException e){
            System.out.println("[AssetService] Can not get value of " + key);
        }
        return r;
    }

    public MultiTypeAsset loadOneRequiredAsset() {
        return loadOneAsset(toBeFoundAsset);
    }

    public MultiTypeAsset loadOneAssetToPublish() {
        return loadOneAsset(toBePublishAsset);
    }

    @Nullable
    private MultiTypeAsset loadOneAsset(String folderName) {
        MultiTypeAsset multiTypeAsset = null;
        List<String> fileList = FileHelper.listFilesForFolder(folderName);
        for (String fileName: fileList) {
            String relativeFilename = folderName + File.separator + fileName;
            String fileContent = FileHelper.loadAssetFromFile(relativeFilename);
            if (!FileHelper.deleteFile(relativeFilename)){
                System.out.println("[AssetService] Fail to delete file " + relativeFilename);
            } else {
                System.out.println("[AssetService] Succeed to delete file " + relativeFilename);
            }
            multiTypeAsset = createFromJson(fileContent);
            if (null != multiTypeAsset) {
                break;
            }
        }
        return multiTypeAsset;
    }
}
