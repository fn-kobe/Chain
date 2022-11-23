package asset.service;

public class AssetServiceThread implements Runnable{
    boolean forceStop = false;
    static AssetServiceThread instance;
    AssetService assetService;
    final int runInterval = 50000;// asset thread is more low priority

    public AssetService getAssetService() {
        return assetService;
    }

    synchronized static public AssetServiceThread getInstance(){
        if (null == instance)
        {
            instance = new AssetServiceThread();
        }
        return instance;
    }

    private AssetServiceThread() {
        this.forceStop = false;
    }

    public boolean isForceStop() {
        return forceStop;
    }

    public void stopService() {
        this.forceStop = true;
    }

    @Override
    public void run() {
        assetService = new AssetService();
        while (!forceStop){
            if (!safeSleep(runInterval)) break;
            assetService.publishAsset();
            assetService.tradeAsset();
        }
    }

    private boolean safeSleep(int milliSeconds) {
        try {
            Thread.sleep(milliSeconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
