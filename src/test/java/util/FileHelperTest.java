package util;

import junit.framework.TestCase;

import java.util.Base64;
import java.util.List;

public class FileHelperTest extends TestCase {

    public void testListFilesForFolder() {
    }

    public void testLoadIPAddressFromFile() {
        String folderName = "toBeFoundAsset";
        List<String> fileList = FileHelper.listFilesForFolder(folderName);
        for (String fileName: fileList) {
            String fileContent = FileHelper.loadAssetFromFile(fileName);
            System.out.println(fileContent);
        }
    }
	
	public void testGetBase64FileString() {
		String testFileName = "test";
		String testFileContent = "test\n\tline2 content\n  line3";
		FileHelper.createFile(testFileName, testFileContent);

		String base64FileString = FileHelper.getBase64FileString(testFileName);
		System.out.println("[TEST][DEBUG] Base64FileString is " + base64FileString);
		String decodedFileContent = new String(Base64.getDecoder().decode(base64FileString));
		System.out.println("[TEST][DEBUG] Decoded content is :\n" + decodedFileContent);
	}
}