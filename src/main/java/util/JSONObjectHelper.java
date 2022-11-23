package util;

import org.json.JSONException;
import org.json.JSONObject;

public class JSONObjectHelper {
    static public Object safeGet(JSONObject object, String key){
        try {
            return object.get(key);
        } catch (JSONException e){
            return null;
        }
    }

    static public String safeGetString(JSONObject object, String key){
        Object o = safeGet(object, key);
        if (null == o){
            return "";
        }
        return (String) o;
    }

    // -1 when error
    static public int safeGetInt(JSONObject object, String key){
        Object o = safeGet(object, key);
        if (null == o){
            return -1;
        }
        return Integer.parseInt(o.toString());
    }

    // -1l when error
    static public Long safeGetLong(JSONObject object, String key){
        Object o = safeGet(object, key);
        if (null == o){
            return -1l;
        }
        return Long.valueOf(o.toString());
    }
}
