package com.droidprojects.simpleimageeditor;

import java.io.FileOutputStream;

import com.droidprojects.simpleimageeditor.BitmapUtils.RotationObserver;
import com.droidprojects.simpleimageeditor.CropBoxView.CropBoxViewListener;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class SimpleImageEditor extends Activity implements CropBoxViewListener{

	// Bundle argument key to retrieve image path
	public static final String 	IMAGE_PATH 			= "image-path";
	// Minimum number of pixels beyond which it will not crop.
	private static final int 	CROP_MIN_PIXELS		=100;
	
	// Keys
	private static final String STATE_IMAGE_PATH 			= "state_key_1";
	private static final String STATE_IMAGE_ORIGINAL_WIDTH 	= "state_key_2";
	private static final String STATE_IMAGE_ORIGINAL_HEIGHT	= "state_key_3";
	private static final String STATE_IMAGE_REGION 			= "state_key_4";
	private static final String STATE_IMAGE_SCALE_FACTOR 	= "state_key_5";
	private static final String STATE_IMAGE_ROTATION		= "state_key_6";
	
	private RelativeLayout 	mRoot;					// Root container
	private RelativeLayout 	mImageContainer;		// Holds the image
	private CropBoxView 	mCropView;				// View that holds the cropping box
	private boolean 		mActivityRecreated=true;// Flag if activity is recreated
	private SaveBitmapTask 	mSaveBitmapTask;		// Task that saves bitmap to disk
	private boolean 		mCropBoxMoved = false; 	// Flag if the crop box has been moved but user didnt crop
	
	// Image data and state vars
	private String 	mImagePath;				// Image path
	private int 	mImageOriginalWidth;	// Original width of the image on disk
	private int 	mImageOriginalHeight;	// Original height of the image on disk
	private Rect 	mCropRegionImage;		// Region on the original image that is shown on screen
	private float 	mScaleFactor;			// Ratio of image resolution  shown on screen vs on disk
	private int 	mRotation;				// Rotation of image
	
	
	/******************************** Activity lifecycle functions *********************************/
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_image_editor);
        
        // Set up the layout
        mRoot = (RelativeLayout) findViewById(R.id.root);
        mImageContainer = (RelativeLayout) findViewById(R.id.image_container);
		mCropView = new CropBoxView(this);
		mImageContainer.addView(mCropView);
		
		// initialise async task
		mSaveBitmapTask = new SaveBitmapTask(this);
		
		if (savedInstanceState!=null){
			
			// Retrieve any saved instance states if activity recreated
			mImagePath=savedInstanceState.getString(STATE_IMAGE_PATH);
			mImageOriginalWidth=savedInstanceState.getInt(STATE_IMAGE_ORIGINAL_WIDTH);
			mImageOriginalHeight=savedInstanceState.getInt(STATE_IMAGE_ORIGINAL_HEIGHT);
			int vals[]=savedInstanceState.getIntArray(STATE_IMAGE_REGION);
			mCropRegionImage = new Rect(vals[0], vals[1], vals[2], vals[3]);
			mScaleFactor=savedInstanceState.getFloat(STATE_IMAGE_SCALE_FACTOR);
			mRotation=savedInstanceState.getInt(STATE_IMAGE_ROTATION);
		}else{
			
			// Get image data and set up state variables if activity newly created
			Intent i = getIntent();
			Bundle extras = i.getExtras();
			if (extras!=null){
				mImagePath = extras.getString(IMAGE_PATH);
			}
			loadOriginalImageData();
			mCropRegionImage = new Rect(0, 0, mImageOriginalWidth, mImageOriginalHeight);
			mRotation=0;
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(STATE_IMAGE_PATH, mImagePath);
		outState.putInt(STATE_IMAGE_ORIGINAL_WIDTH, mImageOriginalWidth);
		outState.putInt(STATE_IMAGE_ORIGINAL_HEIGHT, mImageOriginalHeight);
		int vals[] = {
				mCropRegionImage.left,
				mCropRegionImage.top,
				mCropRegionImage.right,
				mCropRegionImage.bottom
		};
		outState.putIntArray(STATE_IMAGE_REGION, vals);
		outState.putFloat(STATE_IMAGE_SCALE_FACTOR, mScaleFactor);
		outState.putInt(STATE_IMAGE_ROTATION, mRotation);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.simple_image_editor, menu);
		getActionBar().setTitle(R.string.title_edit_image);
		return true;
	}
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		// By now the views have been set up. Load the bitmap if activity has been recreated.
		super.onWindowFocusChanged(hasFocus);
		if (mActivityRecreated){
			mActivityRecreated=false;
			loadTransformedBitmapForView();
		}
	}
	
	
	/****************************** Button Callback functions **************************************/
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == R.id.action_save_image) {
			if (mCropBoxMoved){
				// If the crop box has been moved and save is pressed without cropping
				// then warn the user.
				Toast.makeText(this, R.string.notif_sure_save, Toast.LENGTH_LONG).show();
				mCropBoxMoved=false;
			}else{
				mSaveBitmapTask.execute();
			}
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	public void onCropButtonPressed (View view){
		cropView();
	}
	
	public void onRotate90CWPressed(View view){
		rotateView(90);
	}
	
	public void onRotate90CCWPressed(View view){
		rotateView(-90);
	}
	
	@Override
	public void onBackPressed() {
		finishWithSuccess();
	}
	
	
	/*************************************** All other functions **********************************/
	
	/**
	 * Sets the return code and ends the activity
	 */
	private void finishWithSuccess(){
		setResult(Activity.RESULT_OK);
		finish();
	}

	/**
	 * Loads a part of Original image on disk that is scaled, translated and rotated according
	 * to mCropRegionImage and mRotation. The scale factor is determined by the dimension of 
	 * holding container.
	 */
	private void loadTransformedBitmapForView(){
		
		// Reference to bitmap that has to be recycled.
		Bitmap bmapToRecycle;
		
		// ratio of dimensions of the root container view
	    float ratioRoot = mRoot.getWidth()/(float)mRoot.getHeight();
	    
	    // ratio of dimensions of original bitmap on disk.
		float ratioBitmap = (mCropRegionImage.width())/(float)(mCropRegionImage.height());
		
		// The width and height is calculated to adjust the image within the root view
		int widthInView, heightInView;
		if (ratioBitmap > ratioRoot){
			widthInView = mRoot.getWidth();
			heightInView = (int)(widthInView/ratioBitmap);
		}else{
			heightInView = mRoot.getHeight();
			widthInView = (int)(heightInView*ratioBitmap);
		}
		
		// Scale factor is calculated to load a scaled image
		mScaleFactor = (mCropRegionImage.width())/(float)widthInView;
		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
	    bmOptions.inJustDecodeBounds = false;
	    bmOptions.inSampleSize = Math.round(mScaleFactor);
	    bmOptions.inPurgeable = true;

	    // The scaled and cropped image is loaded.
	    Bitmap bmap = BitmapUtils.loadCroppedBitmap(
	    		mCropRegionImage,
	    		mImageOriginalWidth,
	    		mImageOriginalHeight,
	    		mRotation,
	    		mImagePath,
	    		bmOptions);
	    
	    
	    
	    // The image is then rotated
		Matrix rotationMatrix = new Matrix();
    	rotationMatrix.setRotate(mRotation);
    	bmapToRecycle = bmap;
	    bmap = Bitmap.createBitmap(
	    		bmap, 
	    		0, 
	    		0, 
	    		bmap.getWidth(), 
	    		bmap.getHeight(),
	    		rotationMatrix,
	    		true);
	    BitmapUtils.recycleIfNotSame(bmap, bmapToRecycle);
	    
	    // Loaded image is shown in the view
		LayoutParams params=mImageContainer.getLayoutParams();
		params.width=widthInView;
		params.height=heightInView;
		mImageContainer.setLayoutParams(params);
		mImageContainer.requestLayout();
		mImageContainer.setBackground(new BitmapDrawable(getResources(),bmap));
		mCropView.requestLayout();
	}
	
	/**
	 * Loads the data of the original image on disk.
	 */
	private void loadOriginalImageData(){
		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
	    bmOptions.inJustDecodeBounds = true;
	    BitmapFactory.decodeFile(mImagePath, bmOptions);
	    mImageOriginalWidth = bmOptions.outWidth;
	    mImageOriginalHeight = bmOptions.outHeight;
	}
	
	/**
	 * Applies the transformation to the original image.
	 * @param rotationObserver The observer that is notified of the saving progress.
	 */
	private void saveBitmapToStorage(RotationObserver rotationObserver){
		
		// Load the full resolution cropped image.
		Bitmap bmap = BitmapUtils.loadCroppedBitmap(
				new Rect(mCropRegionImage),
				mImageOriginalWidth,
				mImageOriginalHeight,
				mRotation,
				mImagePath,
				null);
		
		// Apply rotation to the image.
		bmap = BitmapUtils.rotateBitmapAndFree(
				bmap, 
				mRotation,
				this,
				rotationObserver);
	    
		try{
			// Save the image back to the disk
			FileOutputStream out = new FileOutputStream(mImagePath);
		    bmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
		    out.close();
		}catch(Exception ex){
			ex.printStackTrace();
		}

		bmap.recycle();
	}
	
	/**
	 * Applies rotation to the already transformed image in the view.
	 * @param rotation Amount of rotation (90,-90)
	 */
	private void rotateView(int rotation){
		
		// Possible rotation value is +-90. Else return.
		if (rotation!=90 && rotation!=-90) return;
		
		// Translate and then rotate the image
		RectF rect = new RectF(mCropRegionImage);
		Matrix m = new Matrix();
		m.postRotate(rotation);
		m.postTranslate(
				rotation==90  ? mImageOriginalHeight : 0,
				rotation==-90 ? mImageOriginalWidth  : 0);
		m.mapRect(rect);
		mCropRegionImage.set(
				(int)rect.left, 
				(int)rect.top, 
				(int)rect.right, 
				(int)rect.bottom);
		
		// Save rotation amount [0,360)
		mRotation+=rotation;
		mRotation = (mRotation+360)%360;
		
		// Switch image dimensions
		int temp = mImageOriginalHeight;
		mImageOriginalHeight = mImageOriginalWidth;
		mImageOriginalWidth = temp;
		
		// Load the image for view based on new transformation
		loadTransformedBitmapForView();
		
		mCropBoxMoved = false;
	}
	
	/**
	 * Applies crop to the image in the view and loads a new one for the view.
	 */
	private void cropView(){
		
		// Retrieve the rectangle dimensions from crop view
		Rect cropRegionCanvas = mCropView.getCropRegion();
		
		// Create a rect that will hold coordinates of cropRegionCanvas transformed according
		// to the coordinate system of original bitmap.
		Rect cropRegionCanvasTransformed = new Rect();

		// transform the cropRegionCanvas coords to oribinal bitmap coord system.
		cropRegionCanvasTransformed.left = (int)(cropRegionCanvas.left*mScaleFactor + mCropRegionImage.left);
		cropRegionCanvasTransformed.right = (int)(cropRegionCanvas.right*mScaleFactor + mCropRegionImage.left);
		cropRegionCanvasTransformed.top = (int)(cropRegionCanvas.top*mScaleFactor + mCropRegionImage.top);
		cropRegionCanvasTransformed.bottom = (int)(cropRegionCanvas.bottom*mScaleFactor + mCropRegionImage.top);
		
		// If the transformed coords represent a too small region, dont perform the crop and return.
		if (mCropRegionImage.right-mCropRegionImage.left<CROP_MIN_PIXELS ||
				mCropRegionImage.bottom-mCropRegionImage.top<CROP_MIN_PIXELS){
			Toast.makeText(this, R.string.notif_crop_region_too_small, Toast.LENGTH_SHORT).show();
			return;
		}
		
		// Copy the transformed cropRegionCanvas to mCropRegionImage
		mCropRegionImage = cropRegionCanvasTransformed;
		
		// Load the image for view based on new transformation
		loadTransformedBitmapForView();
		
		mCropBoxMoved = false;
	}
	
	
	@Override
	public void onCropBoxMoved() {
		mCropBoxMoved = true;
	}
	
	/************************************** Async Tasks *******************************************/
	
	/**
	 * Saves bitmap to the disk
	 * @author Fouad
	 */
	private class SaveBitmapTask extends AsyncTask<Void, Integer, Void> implements RotationObserver{
		private SimpleImageEditor 	mActivity;
		private ProgressDialog 		mDialog;
		
		public SaveBitmapTask(SimpleImageEditor activity) {
			mActivity = activity;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
			// Set up the progress dialog
			mDialog = new ProgressDialog(mActivity);
	    	mDialog.setMessage(mActivity.getResources().getString(R.string.notif_saving));
	    	mDialog.setCancelable(false);
	    	mDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	    	mDialog.setMax(100);
	    	mDialog.setProgress(0);
	    	mDialog.show();
		}

		@Override
		protected Void doInBackground(Void... params) {
			
			// Start saving the bitmap
			mActivity.saveBitmapToStorage(this);
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			
			// Dismiss the dialog and proceed to finish the activity
			mDialog.dismiss();
			mActivity.finishWithSuccess();
		}
		
		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);
			mDialog.setProgress(values[0]);
		}

		@Override
		public void notifyRotationProgress(int progressPercent) {
			publishProgress(progressPercent);
		}
	}
}
