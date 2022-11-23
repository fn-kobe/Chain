package util;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.ini4j.Ini;

/*
[header]
        key = value
                */
public class IniFileHelper {
    static Logger logger = FileLogger.getLogger();
    Ini ini = null;

    public IniFileHelper() {
    }

    public IniFileHelper(String fileName) {
        init(fileName);
    }

    public boolean init(String fileName){
        try {
            ini = new Ini(new File(fileName));
        } catch (IOException e) {
            logger.warn("Open ini file error:\n" + e.getMessage());
            return false;
        }
        return true;
    }

    public String getValue(String fileName, String section, String key){
        if (init(fileName)){
            return getValue(section, key);
        }
        return "";
    }

    public String getValue(String section, String key){
        if (null == ini){
            logger.error("Ini file not initialized");
            return "";
        }
        return ini.get(section, key);
    }

    public boolean doesSectionExist(String sectionName){
        if (null == ini){
            logger.error("Ini file not initialized");
            return false;
        }
        return null != ini.get(sectionName);
    }
}
