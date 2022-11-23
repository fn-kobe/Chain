package com.scu.suhong.smartcontract.nondeterminacy;

import com.scu.suhong.smartcontract.nondeterminacy.State;
import com.scu.suhong.smartcontract.nondeterminacy.StateSequence;
import com.scu.suhong.smartcontract.nondeterminacy.StateSyncManager;
import org.junit.Test;

import java.util.Date;

public class StateSyncManagerTest {

    @Test
    public void process() {
        // * @param   year    the year minus 1900.
        Date d = new Date(2020-1900,1-1, 2);
        State s1 = new State("s1", d);

        String smartContractName = "smart contract";
        String varietyName = "variety name";
        String syncString = "{\"owner\":\"\",\"SyncTransaction\":\"identifier\",\"blockIndex\":0,\"smartContractName\":\"smart contract\",\"gas\":0,\"h\":\"09C0677A763022672B95D6FEA4AB1898\",\"id\":1060887404,\"varietyName\":\"variety name\",\"value\":\"s1#2020-1-2\"}";
        assert StateSyncManager.process(syncString);
        StateSequence stateSequence = StateSyncManager.getStateSequence(smartContractName, varietyName);
        assert stateSequence.doesStateExist(s1);
        assert 1 == stateSequence.getStateListCopy().size();
        assert stateSequence.isValid();

        assert StateSyncManager.process(syncString);
        assert 1 == stateSequence.getStateListCopy().size();
        assert stateSequence.isValid();

        State s2 = new State("s2", d);
        //s2
        syncString = "{\"owner\":\"\",\"SyncTransaction\":\"identifier\",\"blockIndex\":0,\"smartContractName\":\"smart contract\",\"gas\":0,\"h\":\"09C0677A763022672B95D6FEA4AB1898\",\"id\":1060887404,\"varietyName\":\"variety name\",\"value\":\"s2#2020-1-2\"}";
        assert StateSyncManager.process(syncString);
        assert 2 == stateSequence.getStateListCopy().size();
        assert stateSequence.isValid();

        Date nd = new Date(2020-1900,1-1, 3);
        State s21 = new State("s2", nd);
        //s3
        syncString = "{\"owner\":\"\",\"SyncTransaction\":\"identifier\",\"blockIndex\":0,\"smartContractName\":\"smart contract\",\"gas\":0,\"h\":\"09C0677A763022672B95D6FEA4AB1898\",\"id\":1060887404,\"varietyName\":\"variety name\",\"value\":\"s2#2020-1-3\"}";
        assert !StateSyncManager.process(syncString);
        assert 2 == stateSequence.getStateListCopy().size();
        assert !stateSequence.isValid();

        //Already in error state, no new state can be updated
        State s4 = new State("s4", nd);
        //s4
        syncString = "{\"owner\":\"\",\"SyncTransaction\":\"identifier\",\"blockIndex\":0,\"smartContractName\":\"smart contract\",\"gas\":0,\"h\":\"09C0677A763022672B95D6FEA4AB1898\",\"id\":1060887404,\"varietyName\":\"variety name\",\"value\":\"s4#2020-1-3\"}";
        assert !StateSyncManager.process(syncString);
        assert 2 == stateSequence.getStateListCopy().size();
        assert !stateSequence.isValid();
    }
}