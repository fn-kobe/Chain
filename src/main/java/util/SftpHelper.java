package util;

import com.jcraft.jsch.*;

public class SftpHelper {
    static public boolean getFile(String serverIp, String userName, String password, String fileName){
        JSch jsch = new JSch();
        Session session = null;
        try {
            session = jsch.getSession(userName, serverIp, 22);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setPassword(password);
            session.connect();

            Channel channel = session.openChannel("sftp");
            channel.connect();
            ChannelSftp sftpChannel = (ChannelSftp) channel;
            sftpChannel.get(fileName, fileName);
            sftpChannel.exit();
            session.disconnect();
            return true;
        } catch (JSchException e) {
            e.printStackTrace();
        } catch (SftpException e) {
            e.printStackTrace();
        }
        return false;
    }
}
