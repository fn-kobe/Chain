package com.scu.suhong.dynamic_definition;
        import com.scu.suhong.block.BlockChain;
public class AssetExample extends DynamicalAsset{
    public AssetExample getIntstance(){
        return (AssetExample)BlockChain.getInstance().getGlobalAssetInstance("AssetT11");
    }

    @Override
    public boolean check(){
        String ownerKey = "nextOwner";
        String owner = getIntstance().getValueByKey(ownerKey);
        if (owner.isEmpty()) return true;

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

    void toStringCode(){
        String s = "    @Override\n" +
                "    public boolean check(){\n" +
                "        String ownerKey = \"nextOwner\";\n" +
                "        String owner = getIntstance().getValueByKey(ownerKey);\n" +
                "        if (owner.isEmpty()) return true;\n" +
                "\n" +
                "        return owner.equals(getOwner());\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public void postAction() {\n" +
                "        String ownerKey = \"nextOwner\";\n" +
                "        String currentOwner = getIntstance().getValueByKey(ownerKey);\n" +
                "        String nextOwner = \"\";\n" +
                "        if (currentOwner.isEmpty()) nextOwner = \"owner\" + 1;\n" +
                "        else nextOwner = currentOwner + 1;\n" +
                "        getIntstance().addKeyValue(\"nextOwner\", nextOwner);\n" +
                "    }";
    }

}
