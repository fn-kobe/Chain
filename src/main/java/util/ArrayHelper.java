package util;

import java.util.ArrayList;
import java.util.List;

public class ArrayHelper {
    public static List copy(List source){
        List result = new ArrayList<>();
        for (int i = 0; i < source.size(); ++i){
            result.add(source.get(i));
        }
        return result;
    }
}
