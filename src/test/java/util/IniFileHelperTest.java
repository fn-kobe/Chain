package util;

import junit.framework.TestCase;
import org.ini4j.Config;
import org.junit.Test;

import java.io.*;

/*MiningConfiguration", "hash", "zero_count*/
public class IniFileHelperTest {
    String fileName = "MiningConfiguration";
    String section = "hash";
    String key = "zero_count";
    int value = 6;

    @Test
    public void testGetValueWithAllParameter() throws IOException {
        assert initConfigFile();
        IniFileHelper iniFileHelper = new IniFileHelper();
        assert iniFileHelper.getValue(fileName, section, key).equals(String.valueOf(value));
        removeInitFile();
    }

    @Test
    public void testGetValueFromFileWithSeparateParameter() {
        assert initConfigFile();
        IniFileHelper iniFileHelper = new IniFileHelper();
        iniFileHelper.init(fileName);
        assert iniFileHelper.getValue(section, key).equals(String.valueOf(value));
        removeInitFile();
    }

    boolean initConfigFile(){
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(fileName), "utf-8"));
            writer.write(String.format("[%s]\n", section));
            writer.write(String.format("%s=%d\n", key, value));
            writer.flush();
            writer.close();
            return true;
        }catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    void removeInitFile()
    {
        try{
            File file = new File(fileName);
            if(file.delete()){
                System.out.println(file.getName() + " is deleted!");
            }else{
                System.out.println("Delete operation is failed.");
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}