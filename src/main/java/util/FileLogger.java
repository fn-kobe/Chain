package util;

import org.apache.log4j.*;

public class FileLogger
{
    static Logger logger = null;
    public static Logger getLogger(){

        if (null != logger) return  logger;

        logger = Logger.getLogger("App");
        SimpleLayout layout = new SimpleLayout();
        //FileAppender appender = null;
        ConsoleAppender appender = null;
        try {
            //把输出端配置到out.txt
            //appender = new FileAppender(layout, "blockchain.log", false);
            PatternLayout p = new PatternLayout();
            appender = new ConsoleAppender(p);
        } catch (Exception e) {
            System.out.println("Log error" + e);
            return null;
        }
       logger.addAppender(appender);//添加输出端
        logger.setLevel(Level.DEBUG);//覆盖配置文件中的级别
        return logger;
    }
}
