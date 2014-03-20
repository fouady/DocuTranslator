package com.droidprojects.personaltranslator.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Point;

import com.droidprojects.personaltranslator.constants.Constants;

/**
 * Utility class that contains functions for loading bitmap images.
 * @author Fouad
 */
public class BitmapUtils {

	/**
	 * Image load modes
	 * @author Fouad
	 */
	public enum ImageLoadMode {
		LOAD_FOR_VIEW,	// Load scaled bitmap to be shown on screen.
		LOAD_FOR_OCR, 	// Load scaled and grayscale bitmap for the OCR.
		LOAD_FULL,		// Load the original bitmap without any changes.
	}

	public static Bitmap loadImage(String pathToImageFile, ImageLoadMode mode, Point size) {

		switch (mode) {
		case LOAD_FOR_VIEW: {
			
			// Determine bitmap dimensions
			BitmapFactory.Options bmOptions = new BitmapFactory.Options();
			bmOptions.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(pathToImageFile, bmOptions);
			int photoW = bmOptions.outWidth;

			// Determine scalefactor
			int scaleFactor = photoW / size.x;

			bmOptions.inJustDecodeBounds = false;
			bmOptions.inSampleSize = scaleFactor;
			bmOptions.inPurgeable = true;

			// Load scaled bitmap
			Bitmap bmap = BitmapFactory.decodeFile(pathToImageFile, bmOptions);

			return bmap;
		}
		case LOAD_FOR_OCR: {

			// Determine bitmap dimensions
			BitmapFactory.Options bmOptions = new BitmapFactory.Options();
			bmOptions.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(pathToImageFile, bmOptions);
			int photoW = bmOptions.outWidth;
			int photoH = bmOptions.outHeight;
			int smallerDimension = Math.min(photoW, photoH);

			// Find appropriate scalefactor so that image dims remain within bounds
			int scaleFactor = 1;
			while (smallerDimension / scaleFactor > Constants.OCR_IMAGE_SMALLER_DIM) {
				scaleFactor *= 2;
			}

			bmOptions.inJustDecodeBounds = false;
			bmOptions.inSampleSize = scaleFactor;
			bmOptions.inPurgeable = true;
			
			// Load the bitmap
			Bitmap bmp = BitmapFactory.decodeFile(pathToImageFile, bmOptions);

			int width, height;
			height = bmp.getHeight();
			width = bmp.getWidth();

			// Change bitmap to grayscale
			Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
			Canvas c = new Canvas(bmpGrayscale);
			Paint paint = new Paint();
			ColorMatrix cm = new ColorMatrix();
			cm.setSaturation(0);
			ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
			paint.setColorFilter(f);
			c.drawBitmap(bmp, 0, 0, paint);

			bmp.recycle();

			return bmpGrayscale;
		}
		case LOAD_FULL:
		default:
			return BitmapFactory.decodeFile(pathToImageFile);
		}
	}
}
