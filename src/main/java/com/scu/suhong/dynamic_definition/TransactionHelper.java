package com.scu.suhong.dynamic_definition;

public class TransactionHelper {
    public static String getRebranchDisappearSymbol(){
        return "RD";
    }

    public static String getMarkedDisappearSymbol(){
        return "MD";
    }

    // CrosschainTransaction may disappear as the neighbored blockchain disappeared
    public static boolean markAsRebranchDisappeared(AbstractTransaction transaction){
        if (null == transaction) return false;
        String data = transaction.getData();
        if (data.startsWith(getRebranchDisappearSymbol() + getSymbolSeparator())) return true; //already has been marked

        // Not marked
        transaction.setData(getRebranchDisappearSymbol() + getSymbolSeparator() + data);
        transaction.setHash();
        return true;
    }

    // CrosschainTransaction may disappear as the neighbored blockchain disappeared
    public static boolean markAsDisappeared(AbstractTransaction transaction){
        if (null == transaction) return false;
        String data = transaction.getData();
        if (transaction.doesMarkedAsDisappear()) return true; //already has been marked

        // Not marked
        transaction.setData(getMarkedDisappearSymbol() + getSymbolSeparator() + data);
        transaction.setHash();
        return true;
    }

    public static String getSymbolSeparator(){
        return ":";
    }

    public static boolean isSimilar(AbstractTransaction t1, AbstractTransaction t2){
        // Without is due to rebranch in other blockchain will add prefix or postfix to the data
        return t1.isSimilar(t2);
    }

    public static boolean doesMarkedAsDisappearByNeighborRebranch(AbstractTransaction t){
        return t.getData().startsWith(getRebranchDisappearSymbol()+getSymbolSeparator());
    }

    public static boolean doesMarkedAsDisappearByMarkedFurther(AbstractTransaction t){
        return t.getData().startsWith(getMarkedDisappearSymbol()+getSymbolSeparator());
    }

    public static boolean doesMarkedAsDisappear(AbstractTransaction t){
        return doesMarkedAsDisappearByNeighborRebranch(t) ||
                doesMarkedAsDisappearByMarkedFurther(t) ;
    }

}
