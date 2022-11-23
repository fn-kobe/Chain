package com.scu.suhong.dynamic_definition;

import org.json.JSONObject;
import org.junit.Test;
import util.FileHelper;
import util.TimeHelper;

public class DynamicalAssetTest {

    @Test
    public void testJson() {
        DynamicalAsset dynamicalAsset = new DynamicalAsset();
        assert dynamicalAsset.check();

        dynamicalAsset.addKeyValue("k1", "v1");
        dynamicalAsset.addKeyValue("k2", "v2");
        dynamicalAsset.setSpecifiedDerivedClassName("AssetT1");
        String code = constructCheck("true");
        dynamicalAsset.setCode(code);
        String owner = "owner";
        dynamicalAsset.setOwner(owner);
        String data = "data";
        dynamicalAsset.setData(data);
        int id = 123;
        dynamicalAsset.setId(id);
        int gas = 1000;
        dynamicalAsset.setGas(gas);
        int blockIndex = 123456;
        dynamicalAsset.setBlockIndex(blockIndex);
        dynamicalAsset.setMiningTime(TimeHelper.getEpoch());

        JSONObject object = dynamicalAsset.getJson();
        DynamicalAsset newDynamicalAsset = DynamicalAsset.createFromJson(object);
        assert dynamicalAsset.isSimilar(newDynamicalAsset);
        assert dynamicalAsset.getId() == newDynamicalAsset.getId();
        assert dynamicalAsset.getHash() == newDynamicalAsset.getHash();
    }

    @Test
    public void check() {
        DynamicalAsset dynamicalAsset = new DynamicalAsset();
        assert dynamicalAsset.check();

        String className = "AssetT1";
        //begin to override code
        dynamicalAsset.setSpecifiedDerivedClassName(className);
        FileHelper.deleteFile(className + ".java");
        String code = constructCheck("false");
        dynamicalAsset.setCode(code);

        AssetCompiler assetCompiler = new AssetCompiler();
        assetCompiler.setGivenGas(2000);
        assert assetCompiler.compileAndStartGlobalInstance(className,
                DynamicalAsset.class.getPackage().toString(), code);
        try {
            DynamicalAsset assetT1Object = (DynamicalAsset) assetCompiler.getInstance(className,  DynamicalAsset.class.getPackage().toString());
            assetCompiler.setGivenGas(2000);
            System.out.println("[DynamicalAssetTest][Info] Direct run check");
            assert !assetT1Object.check();
            System.out.println("[DynamicalAssetTest][Info] Run by find");
            assert !assetCompiler.runBooleanMethod(className, "check");
            System.out.println("[DynamicalAssetTest][Info] Succeed to run methods");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("[DynamicalAssetTest][ERROR] exception happens when run methods");
            assert false;
        }
    }

    String constructNewAsset(String className)
    {
        String code = "";
        code += "  public class " + className + " extends DynamicalAsset{\n";
        code += "    @Override\n";
        code += "    public boolean check() {\n";
        code += "        return false;\n";
        code += "    }\n";
        code += "  }\n";
        return code;
    }

    String constructCheck(String result){
        String code = "";
        code += "  public class AssetT1 extends DynamicalAsset{\n";
        code += "    @Override\n";
        code += "    public boolean check() {\n";
        code += "        return " + result + ";\n";
        code += "    }\n";
        code += "  }\n";
        return code;
    }

    @Test
    public void postAction() {
    }
}