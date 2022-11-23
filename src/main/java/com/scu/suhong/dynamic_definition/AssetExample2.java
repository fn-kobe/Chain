package com.scu.suhong.dynamic_definition;
import com.scu.suhong.block.BlockChain;
public class AssetExample2 extends DynamicalAsset{
    public AssetExample2 getIntstance(){
        Object object = BlockChain.getInstance().getGlobalAssetInstance("AssetExample2");
        if (null != object){
            return (AssetExample2) object;
        }
        return null;
    }

    @Override
    public boolean check(){
        String ownerKey = "nextOwner";
        String owner = getIntstance().getValueByKey(ownerKey);
        if (null == owner || owner.isEmpty()) return true;
        return owner.equals(getOwner());
    }

    @Override
    public void postAction() {
        String ownerKey = "nextOwner";
        String currentOwner = getIntstance().getValueByKey(ownerKey);
        String nextOwner = "";
        if (currentOwner.isEmpty()) nextOwner = "owner" + 1;
        else nextOwner = currentOwner + 1;
        getIntstance().addKeyValue("nextOwner", nextOwner);
    }
}
