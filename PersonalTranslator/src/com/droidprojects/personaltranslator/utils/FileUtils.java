package com.droidprojects.personaltranslator.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility functions for managing files.
 * @author Fouad
 */
public class FileUtils {

	/**
	 * Creates an empty image file
	 * @param storageDir The directory where the file will be stored.
	 * @return Metadata of the file created.
	 * @throws IOException
	 */
	public static File createTempImageFile(File storageDir) throws IOException {
		
		// Choose a name based on time stamp
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	    String imageFileName = "JPEG_" + timeStamp + "_";
	    
	    File image = File.createTempFile(
	        imageFileName,  /* prefix */
	        ".jpg",         /* suffix */
	        storageDir      /* directory */
	    );
	    
	    return image;
	}
	
	/**
	 * Replaces toFile with the contents of fromFile.
	 * @param fromFile File that replaces.
	 * @param toFile File that is replaced.
	 */
	public static void overwriteOneFileWithAnother(File fromFile, File toFile){
		FileInputStream inStream=null;
		FileOutputStream outStream=null;
		try {
			inStream = new FileInputStream(fromFile);
			outStream = new FileOutputStream(toFile);
			FileChannel inChannel = inStream.getChannel();
		    FileChannel outChannel = outStream.getChannel();
		    inChannel.transferTo(0, inChannel.size(), outChannel);
		    inStream.close();
		    outStream.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
