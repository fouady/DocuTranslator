package com.droidprojects.simpleimageeditor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;

/**
 * This class provide bitmap utilities used by other classes.
 * @author Fouad
 */
public class BitmapUtils {
	
	private static final String 	TEMP_ROTATION_FILE 	= "_121s3gh45_temp";	// Temp filename
	private static final int 		BUFFER_SIZE			= (2*1024*1024)/4;		// 2 MB buffer size
	
	/**
	 * The class implementing this interface will be updated about the rotation progress
	 * @author Fouad
	 */
	public interface RotationObserver{
		/**
		 * Called when there is an update about the rotation progress
		 * @param progressPercent Rotation progress percentage of the image.
		 */
		public void notifyRotationProgress(int progressPercent); 
	}

	/**
	 * Loads cropped bitmap from the disk.
	 * @param region Region to be cropped
	 * @param imageWidth Width of original image
	 * @param imageHeight Height of original image
	 * @param rotation rotation applied to width and height
	 * @param imagePath Path to the image on disk.
	 * @param opts Options for loading the image
	 * @return
	 */
	public static Bitmap loadCroppedBitmap(
			Rect region, 
			int imageWidth, 
			int imageHeight,
			int rotation,
			String imagePath, 
			BitmapFactory.Options opts){
		
		Bitmap bmap = null;
		try {
			
			BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(imagePath, false);
			
			// inversely rotate the crop region to undo the rotation applied to image width and height
			int invRotation = (-rotation + 360) %360;
			Rect rect = applyRotation(invRotation, imageWidth, imageHeight, region);
			
			// Load the cropped bitmap
			bmap = decoder.decodeRegion(rect, opts);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bmap;
	}
	
	/**
	 * Applies the rotation to a huge image by using storage as buffer instead of RAM.
	 * After rotation, it frees the bitmap to save memory 
	 * @param bitmap Full resolution bitmap to be rotated.
	 * @param rotation Rotation to be applied
	 * @param context Context of the application
	 * @param observer Observer that listens to the progress of rotation.
	 * @return
	 */
	public static Bitmap rotateBitmapAndFree(
			Bitmap bitmap, 
			int rotation, 
			Context context, 
			RotationObserver observer){
		
		// Retrieve image dims
		final int height=bitmap.getHeight();
		final int width=bitmap.getWidth();
		
		try{
			
			// Set up stream for saving to the storage
			final DataOutputStream outputStream=new DataOutputStream(new BufferedOutputStream(context.openFileOutput(TEMP_ROTATION_FILE,Context.MODE_PRIVATE)));
			// Initialize a buffer that will be filled up before its contents are saved to the disk.  
			int[] buffer = new int[BUFFER_SIZE];
			// Number of times buffer filled up
			int buffOffset=0;
			
			// Counter
			int i=0;
			// Loop limits that determine how the image will be rotated
			int startX, startY, incX, incY, limitX, limitY;
			// New dims after rotation
			int newWidth;
			int newHeight;
			// Based on the rotation amount, set the Loop limits
			switch (rotation){
			case 0:
				// No need to rotate
				return bitmap;
			case 90:
				startX=0; 			incX=1; 		limitX=width;
				startY=height-1;	incY=-1; 		limitY=-1;
				newWidth = height;
				newHeight = width;
				break;
			case 180:
				startX=height-1; 	incX=-1; 		limitX=-1;
				startY=width-1; 	incY=-1; 		limitY=-1;
				newWidth = width;
				newHeight = height;
				break;
			case 270:
				startX=width-1; 	incX=-1; 		limitX=-1;
				startY=0; 			incY=1; 		limitY=height;
				newWidth = height;
				newHeight = width;
				break;
			default:
				return null;
			}
			
			// One pixel of the image
			int pixel;
			// Start the loop
			for(int x = startX; x != limitX; x += incX){
				for(int y = startY; y != limitY; y += incY){
					// This determines how the pixels will be read based on the rotation
					switch (rotation/90){
					case 2:
						pixel=bitmap.getPixel(y,x);
						break;
					case 1:
					case 3:
					default:
						pixel=bitmap.getPixel(x,y);
						break;
					}
					// Save pixel to buffer
					buffer[i]=pixel;
					// increment counter
					i++;
					// If the buffer is filled up, write the contents to disk and reset it.
					if (i==BUFFER_SIZE || (buffOffset*BUFFER_SIZE+i)==width*height){
						int progress = Math.min((i*(buffOffset+1)*100)/(2*height*width),100);
						observer.notifyRotationProgress(progress);
						// Convert ints to bytes
						ByteBuffer byteBuffer = ByteBuffer.allocate(i * 4);        
				        IntBuffer intBuffer = byteBuffer.asIntBuffer();
				        intBuffer.put(buffer, 0, i);
				        byte[] bytes = byteBuffer.array();
				        outputStream.write(bytes);
				        i=0;
				        buffOffset++;
					}
				}
			}
			outputStream.flush();
			outputStream.close();
			
			// Free the original bitmap
			bitmap.recycle();

			// Create a new empty bitmap as per new dims
			bitmap=Bitmap.createBitmap(newWidth,newHeight,bitmap.getConfig());
			// Set up an input stream to read the data from disk
			final DataInputStream inputStream=new DataInputStream(new BufferedInputStream(context.openFileInput(TEMP_ROTATION_FILE)));
			
			// reset counter
			i=0;
			// start loop
			for(int y = 0; y < newHeight; y++){
				for(int x = 0; x < newWidth; x++){
					// If buffer is empty, fill it up with new chunk of data from disk.
					if (i%BUFFER_SIZE==0){
						int progress = Math.min((BUFFER_SIZE*(buffOffset+1)*100)/(2*height*width),100);
						observer.notifyRotationProgress(progress);
						
						// convert bytes to int
						byte[] tempBuffer = new byte[BUFFER_SIZE*4];
						int byteCount = inputStream.read(tempBuffer);
						IntBuffer intBuf = ByteBuffer.wrap(tempBuffer, 0, byteCount)
								.order(ByteOrder.BIG_ENDIAN)
								.asIntBuffer();
						intBuf.get(buffer,0,byteCount/4);
						i=0;
						buffOffset++;
					}
					// Copy loaded data to bitmap
					bitmap.setPixel(x, y, buffer[i]);
					i++;
				}
			}
			inputStream.close();
			
			// delete the temporary file
			new File(context.getFilesDir(),TEMP_ROTATION_FILE).delete();
			return bitmap;
	    }catch(final IOException e){
	    	e.printStackTrace();
	    }
		return null;
	}
	
	/**
	 * Frees the bitmap if the two are not same
	 * @param original
	 * @param toRecycle
	 */
	public static void recycleIfNotSame(Bitmap original, Bitmap toRecycle){
		if (original!=toRecycle){
			toRecycle.recycle();
		}
	}
	
	/**
	 * Applies rotation to a region in an image so that after rotation the image's left and top
	 * are 0 and the region is translated accordingly.
	 * always equal to 0.
	 * @param rotation Rotation to be applied
	 * @param imageWidth Width of original image
	 * @param imageHeight Height of original image
	 * @param rect Region inside the original image
	 * @return The transformed region
	 */
	public static Rect applyRotation(int rotation, int imageWidth, int imageHeight, Rect rect){
		Matrix rot = new Matrix();
		rot.setRotate(rotation);
		int dx=0, dy=0;
		switch (rotation){
		case 270:
			dx=0;
			dy=imageWidth;
			break;
		case 180:
			dx=imageWidth;
			dy=imageHeight;
			break;
		case 90:
			dx=imageHeight;
			dy=0;
			break;
		case 0:
			dx=0;
			dy=0;
			break;
		}
		rot.postTranslate(dx, dy);
		RectF rectF = new RectF(rect);
		rot.mapRect(rectF);
		return new Rect((int)rectF.left, (int)rectF.top, (int)rectF.right, (int)rectF.bottom);
	}
}
