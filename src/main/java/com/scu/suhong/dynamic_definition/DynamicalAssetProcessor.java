package com.scu.suhong.dynamic_definition;

import com.scu.suhong.block.Block;

import java.util.List;

public class DynamicalAssetProcessor {
    public static boolean compile(DynamicalAsset dynamicalAsset){
        AssetCompiler assetCompiler = new AssetCompiler();
        assetCompiler.setGivenGas(dynamicalAsset.getGas());
        try {
             return assetCompiler.compile(
                    dynamicalAsset.getSpecifiedDerivedClassName(), "", dynamicalAsset.getCode());
        } catch (ClassCastException e){
            e.printStackTrace();
            return false;
        }
    }

    public static DynamicalAsset compileAndGetInstance(DynamicalAsset dynamicalAsset){
        AssetCompiler assetCompiler = new AssetCompiler();
        assetCompiler.setGivenGas(dynamicalAsset.getGas());
        try {
            dynamicalAsset = (DynamicalAsset) assetCompiler.compileAndGetInstance(
                    dynamicalAsset.getSpecifiedDerivedClassName(), dynamicalAsset.getCode());
        } catch (ClassCastException e){
            e.printStackTrace();
            return null;
        }

        if (null == dynamicalAsset){
            System.out.println("[DynamicalAssetProcessor][ERROR] Init object error");
        }
        return dynamicalAsset;
    }

    public static DynamicalAsset getInstance(String className){
        AssetCompiler assetCompiler = new AssetCompiler();
        DynamicalAsset dynamicalAsset;
        try {
            dynamicalAsset = (DynamicalAsset) assetCompiler.getInstance(className);
        } catch (ClassCastException e){
            e.printStackTrace();
            return null;
        }

        if (null == dynamicalAsset){
            System.out.println("[DynamicalAssetProcessor][ERROR] Init object error");
        }
        return dynamicalAsset;
    }

    public static void processAddFromLongerChain(List<Block> blockList) {
        for (Block block : blockList){
            for (AbstractTransaction t : block.getTransactions()){
                if (t instanceof DynamicalAsset){
                    t.postAction();
                }
            }
        }
    }

    public static void processBlockAdd(Block block) {
        for (AbstractTransaction t : block.getTransactions()){
            if (t instanceof DynamicalAsset){
                t.postAction();
            }
        }
    }

    //  @return initiated instance. For only definition transaction,
    //  return null means compilation failure or the original transaction if OK
    public static DynamicalAsset preProcessDynamicalTransaction(DynamicalAsset transaction) {
        DynamicalAsset newTransaction = transaction;

        if (transaction.isOnlyAssetInitiation()) {   // Only init transaction
            if (null != (newTransaction = DynamicalAssetProcessor.getInstance(newTransaction.getSpecifiedDerivedClassName()))) {
                newTransaction.copy(transaction);
            }
        } else if (transaction.isOnlyAssetDefinition()) {   // Only definition
            if (!DynamicalAssetProcessor.compile(newTransaction)) {
                newTransaction = null;// to indicate compile failure
            }
        } else {   //  definition and init
            if (null != (newTransaction = DynamicalAssetProcessor.compileAndGetInstance(newTransaction))) {
                newTransaction.copy(transaction);
            }
        }

        return newTransaction;
    }

}
