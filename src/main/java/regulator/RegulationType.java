package regulator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum RegulationType {
    ENone(1),
    ESpeed(2),
    EAmount(4),
    ESpeedAmount(8),
    EOff(16);

    int index = 0;

    RegulationType(int i) {
        index = i;
    }
}
