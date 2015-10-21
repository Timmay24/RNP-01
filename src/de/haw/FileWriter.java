package de.haw;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileWriter {
	
	public FileWriter() {
		super();
	}

	public static void writeStringToFile(String string, String filePath) {
		FileOutputStream fop = null;
		try {
    		File file = new File(filePath);
    		if(!file.exists()){
    		   file.createNewFile();
    		}
    		fop = new FileOutputStream(file);
    		fop.write(string.getBytes());
    	} catch(IOException e){
    		e.printStackTrace();
    	} finally {
    		try {
				fop.flush();
			} catch (IOException e) {
				// ignore
				e.printStackTrace();
			}
    		try {
				fop.close();
			} catch (IOException e) {
				// ignore
				e.printStackTrace();
			}
    		
    	}
    }
}