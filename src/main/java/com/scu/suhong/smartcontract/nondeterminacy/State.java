package com.scu.suhong.smartcontract.nondeterminacy;

import org.json.JSONObject;
import util.DateHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class State {
    String s = "";
    Date t;

    public State(String s, Date t) {
        this.s = s;
        this.t = t;
    }

    public State(String stateTime) {
        if (stateTime.isEmpty()) return;

        String[] stateTimeArray = stateTime.split(State.getStringSeparator());
        if (2 == stateTimeArray.length) {
            this.s = stateTimeArray[0];
            SimpleDateFormat dateFormat = DateHelper.getSimpleDateFormat();
            try {
                this.t = dateFormat.parse(stateTimeArray[1].replaceAll("\\r\\n|\\r|\\n", ""));
            } catch (ParseException e) {
                e.printStackTrace();
                this.t = null;
            }
        } else {
            System.out.println("[State][ERROR] string format error, empty state constructed");
        }
    }

    // In same cases, s and t are combined with separator to pass
    // For example, s1<Separator>2020_01_01
    // Then s and t should have no such separator
    public static String getStringSeparator(){
        return "#";
    }

    public boolean isTheSame(State antoher){
        return s.equals(antoher.s) && t.equals(antoher.t);
    }

    // 0 no fields are the same;
    // 1 state is  not same while date is the same; 2 state is the same while date is not the same.
    // 3 both fields are the same;
    public int compareState(State another){
        if (!s.equals(another.s))
        {
            // s is not same
            if (!t.equals(another.t)){
                return 0; // no fields are the same
            }
            return 1; // state is  not same while date is the same
        }

        // s is the same
        if (!t.equals(another.t)){
            return 2; // state is the same while date is not the same
        }
        return 3; // both fields are the same
    }

    JSONObject getJson(){
        JSONObject object = new JSONObject();
        object.put("state", s);
        SimpleDateFormat dateFormat = DateHelper.getSimpleDateFormat();
        object.put("time", dateFormat.format(t));
        return object;
    }

    public static State fromJson(JSONObject object){
        String s = object.getString("state");
        SimpleDateFormat dateFormat = DateHelper.getSimpleDateFormat();
        Date date = null;
        try {
            date = dateFormat.parse(object.getString("time"));
            return new State(s, date);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getSate() {
        return s;
    }

    public Date getTime() {
        return t;
    }

    public String getValue(){
        return s + getStringSeparator() + DateHelper.getSimpleDateFormat().format(t);
    }
}
