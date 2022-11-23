package Service;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

// public interface for the outer user
public interface BlockchainInterface {
    void startService();

    void triggerTransaction(List<String> arguments) throws IOException;

    void triggerTransaction(List<String> arguments, boolean isInternalTx) throws IOException;

    void triggerCommonCrosschainTransaction(List<String> arguments) throws IOException;

    void triggerStateSyncTransaction(List<String> arguments) throws IOException;

    void updateNBSState(String smartContractName, String varietyName, String value)  throws IOException;

    void triggerStateSyncTransaction(String smartContractName, String varietyName, String value)  throws IOException;

    void triggerConditionalTransaction(List<String> arguments) throws IOException;

    // from incomingAddress paymentAddress value contractNumber
    void triggerExternalTransaction(List<String> arguments) throws IOException;

    // from incomingAddress paymentAddress value contractNumber
    void triggerMultipleTypeTransaction(List<String> arguments) throws IOException;

    // exchangeid <fix|var> from to assetType assetAmount "from:<to>:assetType:assetAmount;from:<to>:assetType:assetAmount;..."
    void triggerCrosschainMultipleTypeTransaction(List<String> arguments) throws IOException;

    // sendcmt exchangeid <fixed|variable> from to assetType assetAmount "requiredChainID:from:<to>:assetType:assetAmount;requiredChainID:from:<to>:assetType:assetAmount;..." data
    void triggerSendACPTriggerTransaction(List<String> arguments) throws IOException;

    // sendacptrigger incomingName totalSuccessiveActionNumber maxAllowedActionNumber maxWaitingTime data incomingOwner
    void triggerSendACPActionSettingTransaction(List<String> arguments) throws IOException;

    // sendcmt exchangeid <fixed|variable> from to assetType assetAmount "requiredChainID:from:<to>:assetType:assetAmount;requiredChainID:from:<to>:assetType:assetAmount;..."
    void triggerDynamicalDefinitionTransaction(List<String> arguments) throws IOException;

    // sendcmt exchangeid <fixed|variable> from to assetType assetAmount "requiredChainID:from:<to>:assetType:assetAmount;requiredChainID:from:<to>:assetType:assetAmount;..." data
    void triggerOverrideAndInitTransaction(List<String> arguments) throws IOException;

    // senddoit owner gas classname key:value;key:value "data" "code"
    void triggerDynamicalInitTransaction(List<String> arguments) throws IOException;

    void triggerInternalSmartContract(List<String> arguments) throws IOException;

    void stopService();
}
