package util;

import com.scu.suhong.block.Block;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class FileHelper {
    static public List<String> listFilesForFolder(String folderName) {
        List<String> fileList = new ArrayList<>();
        if (!createFolderIfNotExist(folderName)){
            System.out.println("[FileHelper] Folder cannot be created of " + folderName);
            return fileList;
        }
        File folder = new File(folderName);
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                System.out.println("[FileHelper] skip folder");
            } else {
                System.out.println("[FileHelper] Find file " + fileEntry.getName() + " in folder of " + folderName);
                fileList.add(fileEntry.getName());
            }
        }
        if (folder.listFiles().length > 0) {
            System.out.println("[FileHelper] Succeed to find the file in " + folderName + " with file count: " + folder.listFiles().length);
        }
        return fileList;
    }

    static public boolean doesFolderContain(String folderName, String flag){
        List<String> files = listFilesForFolder(folderName);
        for (String f : files){
            if (doesFileContain(folderName + File.separator + f, flag)) return true;
        }
        return false;
    }

    static public boolean doesFileContain(String fileName, String flag){
        FileReader fileReader = null;
        BufferedReader br = null;
        try {
            fileReader = new FileReader(new File(fileName));
            br = new BufferedReader(fileReader);
            String line = null;
            // if no more lines the readLine() returns null
            while ((line = br.readLine()) != null) {
                // reading lines until the end of the file
                if (line.contains(flag)) return true;
            }
        } catch (FileNotFoundException e) {
            System.out.println("[FileHelper] [WARN] exception  \"FileNotFoundException\" happened when try read file " + fileName);
        } catch (IOException e) {
            System.out.println("[FileHelper] [WARN] exception \"IOException\" happened when try read file " + fileName);
        } finally {
            safeClose(fileReader, br);
        }

        return false;
    }

    static public boolean createFolderIfNotExist(String folderName) {
        File dir = new File(folderName);
        if (dir.exists()) {
            return true;
        }
        return dir.mkdir();
    }

    static public boolean doesFileOrFolderExist(String name) {
        File file = new File(name);
        return file.exists();
    }

    static public boolean createFile(String fileName, String content) {
        return createFile(fileName, content, true);
    }

    static public boolean renameFile(String oldName, String newName){
        File of = new File(oldName);
        File nf = new File(newName);
        return of.renameTo(nf);
    }

    static public boolean createFile(String fileName, String content, boolean isDebugLogout) {
        FileOutputStream file;
        try {
            file = new FileOutputStream(fileName);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("[FileHelper][ERROR] failed to create file " + fileName);
            return false;
        }
        boolean r =false;
        try {
            file.write(content.getBytes());
            r = true;
            if (isDebugLogout) System.out.println("[FileHelper] Succeed to save file " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("[FileHelper][ERROR] Failed to save file" + fileName);
            r = false;
        } finally {
            safeCloseFile(file);
        }
        return r;
    }

    private static void safeCloseFile(FileOutputStream file) {
        try {
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static public String loadAssetFromFile(String fileName) {
        return loadAssetFromFile(fileName, true);
    }

    static public String loadAssetFromFile(String fileName, boolean doesDebugInformationLogout) {
        return loadContentFromFile(fileName, doesDebugInformationLogout);
    }

    static public String loadContentFromFile(String fileName) {
        return loadContentFromFile(fileName, true);
    }

    static public String loadContentFromFile(String fileName, boolean doesDebugInformationLogout) {
        String fileCotent = "";

        if (doesDebugInformationLogout) System.out.println("[FileHelper] Begin to load " + fileName);
        FileReader fileReader = null;
        BufferedReader br = null;
        try {
            fileReader = new FileReader(new File(fileName));
            br = new BufferedReader(fileReader);
            String line = null;
            // if no more lines the readLine() returns null
            while ((line = br.readLine()) != null) {
                // reading lines until the end of the file
                line.trim();
                fileCotent += line;
                fileCotent += "\n";
            }
        } catch (FileNotFoundException e) {
            if (doesDebugInformationLogout) System.out.println("[FileHelper] [WARN] exception  \"FileNotFoundException\" happened when try read file " + fileName);
        } catch (IOException e) {
            System.out.println("[FileHelper] [WARN] exception \"IOException\" happened when try read file " + fileName);
        } finally {
            safeClose(fileReader, br);
        }

        if (doesDebugInformationLogout) System.out.println("[FileHelper] Finish to load " + fileName);
        return fileCotent;
    }

    static public boolean deleteFolder(String folderName) {
        File dir = new File(folderName);
        return dir.delete();
    }

    static public boolean deleteFile(String fileName) {
        File dir = new File(fileName);
        return dir.delete();
    }

    static public boolean copyFileByForce(String s, String t) {
        File sf = new File(s);
        File tf = new File(t);
        try {
            Files.copy(sf.toPath(), tf.toPath(), REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void safeClose(FileReader fileReader, BufferedReader br) {
        try {
            if(null != br) {
                br.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if(null != fileReader) {
                fileReader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getBase64FileString(String fileName){
        try {
            FileInputStream fileInputStream = new FileInputStream(new File(fileName));
            byte[] read = new byte[fileInputStream.available()];
            fileInputStream.read(read);
            fileInputStream.close();
            return Base64.getEncoder().encodeToString(read);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getFileNameByClassOrFileName(String fileOrClassName){
        String javaFilePostFix = ".java";
        if (fileOrClassName.endsWith(javaFilePostFix)){
            return fileOrClassName;
        }
        return fileOrClassName + javaFilePostFix;
    }

    public static String getFileNameByClassOrFileName(String fileOrClassName, String path){
        String javaFilePostFix = ".java";
        if (fileOrClassName.endsWith(javaFilePostFix)){
            return path + File.separator + fileOrClassName;
        }
        return path + File.separator + fileOrClassName + javaFilePostFix;
    }

}
