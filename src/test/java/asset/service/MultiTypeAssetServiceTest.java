package asset.service;

import Service.BlockchainService;
import Service.BlockchainServiceThread;
import asset.MultiTypeAsset;
import asset.Big_data;
import asset.Personal_data;
import org.json.JSONObject;
import org.junit.Test;
import util.FileHelper;
import util.StringHelper;
import util.TimeHelper;

import java.io.File;
import java.io.IOException;

public class MultiTypeAssetServiceTest {

    @Test
    public void testLoadOneRequiredAsset() {
        String folderName = AssetService.getToBeDiscoverAssetFolderName();
        String fileName = "testPersonalDataRequired";
        Personal_data requiredData = new Personal_data("Person 1", "Henry", "ZhangSan chengdu", "00EF123E");
        JSONObject object = requiredData.getJson();
        String asset_content = object.toString();
        FileHelper.createFile(folderName + File.separator + fileName, asset_content);

        AssetService assetService = new AssetService();
        MultiTypeAsset multiTypeAsset = assetService.loadOneRequiredAsset();
        assert multiTypeAsset.getName().equals("Person 1");
        assert multiTypeAsset.getType().equals("personal_data");
    }

    @Test
    public void testDiscoveryAsset() throws IOException, InterruptedException {
        // publish first
        publishAsset();
        // discovery
        createToBeDiscoverAssetFile();

        AssetService assetService = new AssetService();
        MultiTypeAsset multiTypeAsset = assetService.discoveryAsset();

        assert null != multiTypeAsset;
        assert multiTypeAsset.negotiation(multiTypeAsset.getPrice() + 1);
        assert multiTypeAsset.transfer(multiTypeAsset.getType(), "Mike");

        stopBlockchainService();
    }

    @Test
    public void testTradeAsset() throws IOException, InterruptedException {
        // publish first
        publishAsset();
        // discovery
        createToBeDiscoverAssetFile();

        AssetService assetService = new AssetService();
        AssetService.setBuyer("testBuyer");
        AssetService.setBuyerKey("testBuyerKey");
        assert assetService.tradeAsset();

        stopBlockchainService();
    }

    private void createToBeDiscoverAssetFile() throws InterruptedException {
        Personal_data testData = new Personal_data("Person 1", "Henry", "ZhangSan chengdu", "00EF123E");
        String fileName = "." + File.separator + AssetService.getToBeDiscoverAssetFolderName() + File.separator + "testBeDiscoverAsset";
        FileHelper.createFile(fileName, testData.getJson().toString());
        Thread.sleep(1000); // sleep until pushed
    }


    private void publishAsset() throws IOException, InterruptedException {
        createPublishAssetFile();
        startBlockchainService();

        Thread.sleep(1000);//sleep 1 seconds
        AssetService assetService = new AssetService();
        assert AssetPublishResult.EOK == assetService.publishAsset();
        Thread.sleep(1000);//sleep 1 seconds
    }

// For test separate data storage
    @Test
    public void publishPersonalAsset() throws IOException, InterruptedException {
        startBlockchainService();
        AssetService assetService = new AssetService();

        for (int i = 0; i < 1; ++i) {//
            Personal_data personal_data = generatePersonalData();
            assert AssetPublishResult.EOK == assetService.publishAsset(personal_data);
            waitMinerToFinish();//sleep some seconds
        }
    }

    // For test separate data storage
    @Test
    public void publishBigDataAsset() throws IOException, InterruptedException {
        startBlockchainService();
        AssetService assetService = new AssetService();

        for (int i = 0; i < 1; ++i) {
            Big_data big_data = generateBigData();
            assert AssetPublishResult.EOK == assetService.publishAsset(big_data);
            waitMinerToFinish();//sleep some seconds
        }
    }

    private void waitMinerToFinish() throws InterruptedException {
        System.out.println("Begin to wait miner to mine");
        Thread.sleep(7000);
        System.out.println("End to wait miner to mine");
    }

    //In Paper experiment
    // temporary remove this Test as connection will not be setup
    //@Test
    public void testTradeAssetBigDataWithServalTimes() throws InterruptedException {
        startBlockchainService();

        NameTime[] nameTimes = new NameTime[6];
        nameTimes[0] = new NameTime();
        nameTimes[1] = new NameTime();
        nameTimes[2] = new NameTime();
        nameTimes[3] = new NameTime();
        nameTimes[4] = new NameTime();
        nameTimes[5] = new NameTime();

        nameTimes[0].setName("9M");
        nameTimes[0].setTime(10);

        nameTimes[1].setName("56M");
        nameTimes[1].setTime(15);

        nameTimes[2].setName("100M");
        nameTimes[2].setTime(20);

        nameTimes[3].setName("300M");
        nameTimes[3].setTime(40);

        nameTimes[4].setName("900M");
        nameTimes[4].setTime(180);

        nameTimes[5].setName("8.2G");
        nameTimes[5].setTime(600);

        for (int i = 5; i < 6; ++i){
            System.out.println("[TEST][DEBUG]Begin to get " + nameTimes[i].getName());
            testTradeAssetBigData(nameTimes[i].getName());
            Thread.sleep(nameTimes[i].getTime()*1000); // 10 minutes
        }

        Thread.sleep(10*1000); //
    }

    //In Paper experiment
    @Test
    public void testTradeAssetBigData() throws InterruptedException {
        startBlockchainService();
        testTradeAssetBigData("p.js");
        waitMinerToFinish();
    }

    public void testTradeAssetBigData(String fileName) throws InterruptedException {
        return;//  as no connect server is available

//        AssetService assetService = new AssetService();
//        Big_data generatedBig_data =  generateBigData();// used both for publish and discover
//        generatedBig_data.setServerIp("192.168.30.131");
//        generatedBig_data.setUserName("suhong");
//        generatedBig_data.setPassword("scu123");
//        generatedBig_data.setFileName(fileName);
//
//        Big_data publishBig_data = new Big_data(generatedBig_data);
//        assetService.publishAsset(publishBig_data);
//        waitMinerToFinish();
//
//        Big_data toBeDiscoveredBig_data = new Big_data(generatedBig_data);
//        System.out.println("[MultiTypeAssetServiceTest][DEBUG][EPOCH] Begin to calculate time: " + TimeHelper.getEpoch());
//        Big_data big_data = (Big_data) assetService.discoveryAsset(toBeDiscoveredBig_data);
//        assert (null != big_data);
//
//        assert (big_data.negotiation(big_data.getPrice() + 0));
//
//        assert big_data.transfer(big_data.getType(), "testBuyer", "testBuyerKey");
//        System.out.println("[MultiTypeAssetServiceTest][DEBUG][EPOCH] End of the calculate time: " + TimeHelper.getEpoch());
    }

    //In Paper experiment
    @Test
    public void testTradeAssetPersonalDataWithServalTimes() throws InterruptedException, IOException {
        for (int i = 0; i < 1; ++i){
            testTradeAssetPersonalData();
        }
    }

    //In Paper experiment
    @Test
    public void testTradeAssetPersonalData() throws InterruptedException, IOException {
        startBlockchainService();

        AssetService assetService = new AssetService();
        Personal_data generatePersonalData =  generatePersonalData();// used both for publish and discover

        Personal_data publishPersonal_data = new Personal_data(generatePersonalData);
        assetService.publishAsset(publishPersonal_data);
        waitMinerToFinish();

        System.out.println("[MultiTypeAssetServiceTest][DEBUG][EPOCH] Begin to calculate time: " + TimeHelper.getEpoch());
        Personal_data toBeDiscoveredPersonal_data = new Personal_data(generatePersonalData);
        Personal_data personal_data = (Personal_data) assetService.discoveryAsset(toBeDiscoveredPersonal_data);
        assert (null != personal_data);

        assert (personal_data.negotiation(personal_data.getPrice() + 0));

        assert personal_data.transfer(personal_data.getType(), "testBuyer");
        waitMinerToFinish();
    }

    private Big_data generateBigData() {
        String name = StringHelper.generateRandomString('n', 30, 3);
        String ownerName = StringHelper.generateRandomString('o', 30, 3);
        String keyWord = StringHelper.generateRandomString('k', 30, 3);
        String hash = StringHelper.generateRandomString('h', 8, 8);
        String size = StringHelper.generateIntString( 30, 3);
        int price = StringHelper.generateInt(30,1);

        String serverIp = StringHelper.generateRandomString('i', 15, 7);
        String userName = StringHelper.generateRandomString('o', 30, 3);
        String password = StringHelper.generateRandomString('o', 30, 3);
        String fileName = StringHelper.generateRandomString('o', 30, 3);

       Big_data big_data = new Big_data(name, ownerName, keyWord, hash);
       big_data.setSize(size);
       big_data.setPrice(price);
        big_data.setServerIp(serverIp);
        big_data.setUserName(userName);
        big_data.setPassword(password);
        big_data.setFileName(fileName);

       return big_data;
    }

    private Personal_data generatePersonalData() {
        String name = StringHelper.generateRandomString('n', 30, 3);
        String ownerName = StringHelper.generateRandomString('o', 30, 3);
        String keyWord = StringHelper.generateRandomString('k', 30, 3);
        String phoneNumber = StringHelper.generateRandomString('p', 30, 3);
        String hash = StringHelper.generateRandomString('h', 8, 8);
        String address = StringHelper.generateRandomString('a', 30, 3);
        int price = StringHelper.generateInt(30,1);

        Personal_data personal_data = new Personal_data(name, ownerName, keyWord, hash);
        personal_data.setPhoneNumber(phoneNumber);
        personal_data.setAddress(address);
        personal_data.setPrice(price);

        return personal_data;
    }

    private void createPublishAssetFile() {
        String folderName = AssetService.getToBePublishAssetFolderName();
        String fileName = "testPersonalDataToBePublished";
        Personal_data requiredData = new Personal_data("Person 1", "Henry", "ZhangSan chengdu","00EF123E");
        JSONObject object = requiredData.getJson();
        String asset_content = object.toString();
        FileHelper.createFolderIfNotExist(folderName);
        FileHelper.createFile(folderName + File.separator + fileName, asset_content);
    }

    private void stopBlockchainService() {
        BlockchainService.getInstance().stopService();
    }

    private void startBlockchainService() throws InterruptedException {
        // Have to start the service as it may not be started
        BlockchainServiceThread blockchainServiceThread = new BlockchainServiceThread();
        Thread blockchainThread = new Thread(blockchainServiceThread,"Test blockchain service thread");
        blockchainThread.start();
        Thread.sleep(2000);//in seconds
    }

    class NameTime{
        public String name;
        public int time;

        public NameTime() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getTime() {
            return time;
        }

        public void setTime(int time) {
            this.time = time;
        }
    }
}