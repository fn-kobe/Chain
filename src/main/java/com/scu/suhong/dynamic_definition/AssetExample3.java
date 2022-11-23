package com.scu.suhong.dynamic_definition;
import com.scu.suhong.block.BlockChain;
class NewAsset1 extends DynamicalAsset{
    @Override
    public boolean check(){
        return true;
    }

    @Override
    public void postAction(){
        // No special action
    }
}