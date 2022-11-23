package Service;

public class BlockchainServiceThread implements Runnable {
    BlockchainService blockchainService;

    public BlockchainServiceThread() {
    }

    public BlockchainServiceThread(BlockchainService blockchainService) {

        this.blockchainService = blockchainService;
    }

    @Override
    public void run() {
        if(null == blockchainService)
        {
            blockchainService = BlockchainService.getInstance();
        }
        System.out.println("[BlockchainServiceThread] block chain service start");
        blockchainService.startService();
        System.out.println("[BlockchainServiceThread] block chain service end");
    }
}
