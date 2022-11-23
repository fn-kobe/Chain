package com.scu.suhong.smartcontract.nondeterminacy;

import com.scu.suhong.smartcontract.nondeterminacy.State;
import com.scu.suhong.smartcontract.nondeterminacy.StateSequence;
import org.junit.Test;

import java.util.Date;

public class StateSequenceTest {

    @Test
    public void add() {
        StateSequence stateSequence = new StateSequence();
        State s1 = new State("s1", new Date(2020 - 1900,1, 2));
        State s2 = new State("s1", new Date(2020 - 1900,1, 3));
        State s3 = new State("s3", new Date(2020 - 1900,1, 4));

        assert stateSequence.add(s1);
        assert 1 == stateSequence.getStateNumber();
        assert stateSequence.add(s1);
        assert 1 == stateSequence.getStateNumber();
        assert !stateSequence.add(s2);
        assert 1 == stateSequence.getStateNumber();
        assert stateSequence.add(s3);
        assert 2 == stateSequence.getStateNumber();
    }

    @Test
    public void checkStatus() {
        State s1 = new State("s1", new Date(2020 - 1900,1, 2));
        assert 3 == s1.compareState(s1);

        State s2 = new State("s1", new Date(2020 - 1900,1, 3));
        assert 2 == s1.compareState(s2);

        State s3 = new State("s3", new Date(2020 - 1900,1, 4));
        assert 0 == s1.compareState(s3);

    }
}