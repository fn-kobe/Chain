package asset;

import Service.BlockchainService;
import org.junit.*;
import mockit.*;

import org.json.JSONObject;

import java.io.IOException;

public class MultiTypeAssetTest {

    @Test
    public void testPublish(@Mocked final BlockchainService blockchainService) throws IOException {
/*        new Expectations() {{
            blockchainService.triggerTransaction();
        }};*/
        MultiTypeAsset multiTypeAsset = new MultiTypeAsset("asset1", "Henry Su");
        assert multiTypeAsset.publish();
    }

    @Test
    public void testGetDescription() {
        MultiTypeAsset multiTypeAsset = new MultiTypeAsset("asset1", "Henry Su");
        String description = multiTypeAsset.getDescription();
        JSONObject object = new JSONObject(description);
        assert object.get("name").equals("asset1");
        assert object.get("ownerName").equals("Henry Su");
    }

    @Test
    public void testNegotiation() {
        MultiTypeAsset multiTypeAsset = new MultiTypeAsset("assetNegotiation", "Henry Su");
        multiTypeAsset.setPrice(3);
        assert !multiTypeAsset.negotiation(2);
        assert multiTypeAsset.negotiation(3);
        assert multiTypeAsset.negotiation(4);
    }

    @Test
    public void testTransfer(@Mocked final  BlockchainService blockchainService) throws IOException {
/*        new Expectations() {{
            blockchainService.triggerTransaction(anyString);
        }};*/
        MultiTypeAsset multiTypeAsset = new MultiTypeAsset("assetNegotiation", "Henry Su");
        assert multiTypeAsset.transfer("asset", "Jacky Wang");
    }
}