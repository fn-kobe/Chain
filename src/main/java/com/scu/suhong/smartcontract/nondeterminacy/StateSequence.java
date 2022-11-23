package com.scu.suhong.smartcontract.nondeterminacy;

import java.util.ArrayList;
import java.util.List;

// state is a sequence now
// We assume state can be differentiate. i.e. no duplicated state
public class StateSequence {
    List<State> stateList = new ArrayList<>();
    boolean isValid = true;

    public boolean isValid() {
        return isValid;
    }

    public int getStateNumber(){
        return stateList.size();
    }

    public boolean doesStateExist(State state){
        for (State s : stateList){
            if (s.isTheSame(state)) return true;
        }
        return false;
    }

    public List<State> getStateListCopy(){
        List<State> stateListCopy = new ArrayList<>();
        for (State s : stateList){
            stateListCopy.add(s);
        }
        return stateListCopy;
    }

    public boolean add(State newState){
        dumpState();

        if (!isValid){
            System.out.println("[StateSequence][ERROR] Conflict state from NBS. Skip to process its further state");
            return false;
        }

        int checkStatus= checkStatus(newState);
        if (2 ==checkStatus){
            System.out.println("[StateSequence][ERROR] State exists while the timestamp is different, error condition");
            System.out.println("[StateSequence][ERROR] Error states got from NBS, mark internal state as invalid!");
            isValid = false;
            return false;
        }
        if (1 == checkStatus){
            System.out.println("[StateSequence][INFO] State already exits, skip to add");
            return true;
        }

        int pos = 0;
        for (State s: stateList){
            if (newState.t.after(newState.t)) break;
            ++pos;
        }
        stateList.add(pos, newState);
        System.out.println("[StateSequence][INFO] Succeed to add state " + newState.getJson().toString());
        dumpState();
        return true;
    }

    // 0 no state exits; 1 duplicated state; 2 states exists while date is not the same.
    int checkStatus(State newState){
        for (State s : stateList){
            int compareState = s.compareState(newState);
            //2 state is the same while date is not the same.
            if (2 == compareState) return 2;

            // 3 both fields are the same;
            if (3 == compareState) return 1;
        }
        return 0;
    }

    public void dumpState(){
        String d = "[StateSequence][Dump] Begin to dump the NBS state\n";
        for (State s : stateList){
            d += s.getJson().toString();
            d += "\n";
        }
        d += "dump end";
        System.out.println(d);
    }
}
