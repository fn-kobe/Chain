package com.scu.suhong.smartcontract.nondeterminacy;

import com.scu.suhong.smartcontract.nondeterminacy.State;
import org.json.JSONObject;
import org.junit.Test;
import util.DateHelper;

import java.text.SimpleDateFormat;
import java.util.Date;

public class StateTest {

    @Test
    public void fromJson() {
        State s1 = new State("s1", new Date(2020 - 1900,1, 2));
        JSONObject s1Json = s1.getJson();
        System.out.println(s1Json.toString());

        State s2 = State.fromJson(s1Json);
        assert s2.isTheSame(s1);
        State s3 = new State("s1", new Date(2020 - 1900,1, 3));
        State s4 = new State("s3", new Date(2020 - 1900,1, 2));
        assert !s3.isTheSame(s1);
        assert !s4.isTheSame(s1);
    }

    @Test
    public void testConstruct() {
        SimpleDateFormat dateFormat = DateHelper.getSimpleDateFormat();
        Date date = new Date(2020-1900, 1, 2);
        String dataString = dateFormat.format(date);
        String state = "state1";

        State s1 = new State(state + State.getStringSeparator() + dataString);
        State s2 = new State(state, date);
        System.out.println("[Test] value string is " + s2.getValue());

        assert s2.isTheSame(s1);
        assert s2.getTime().equals(s1.getTime());
        assert s2.getSate().equals(s1.getSate());
        assert s2.getJson().toString().equals(s1.getJson().toString());
    }
}