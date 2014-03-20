package com.droidprojects.personaltranslator.views;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import com.droidprojects.personaltranslator.R;
import com.droidprojects.personaltranslator.constants.Constants;
import com.droidprojects.personaltranslator.controllers.ConnectionHandler;
import com.droidprojects.personaltranslator.controllers.Controller;
import com.droidprojects.personaltranslator.customclasses.CustomAsyncTask;
import com.droidprojects.personaltranslator.utils.BitmapUtils;
import com.droidprojects.personaltranslator.utils.FileUtils;
import com.droidprojects.personaltranslator.views.OCRLanguagePickerDialogFragment.OCRLanguagePickerDialogListener;
import com.droidprojects.personaltranslator.views.FragmentHolderActivity.FragmentHolderListener;
import com.droidprojects.simpleimageeditor.SimpleImageEditor;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.location.Geocoder;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The purpose of this fragment is to enable the user to take the image
 * and edit it before the OCR can be run. 
 * @author Fouad
 */
public class PhotoFragment extends Fragment implements 
									OCRLanguagePickerDialogListener,
									FragmentHolderListener{
	
	// Views from the layout
	private TextView 		mTapCameraMessageTextView;	// Holds the message to take photo
	private Button 			mButton1;					// Reference to first button in parent activity
	private Button 			mButton2;					// Reference to second button in parent activity
	private ImageView		mPhotoImageView;			// Holds the image containing text
	private RelativeLayout 	mPhotoFragmentRootLayout;	// The root layout in the fragment
	
	// Request codes
	private final static int REQUEST_TAKE_PHOTO = 1;
	private final static int REQUEST_EDIT_PHOTO = 2;
	
	// file paths
	private String mCurrentImagePath;	// Path to the current image on disk
	private String mTempImagePath=null;	// Path to the temp image on disk
	
	// state variables
	private boolean mImageAvailableOnPath=false;	// If an image is available on current path
	private static int sSelectedLanguageIndex=0;	// Index of the selected language. 0 is default.
	
	/**
	 * The class implementing this listener interface is called once the fragment
	 * is done extracting text 
	 * @author Fouad
	 */
	public interface PhotoFragmentListener{
		/**
		 * Called when text has been extracted
		 * @param extractedText
		 * @param selectedLanguageIndex Index of the language of text as picked by the user
		 */
		public void onTextExtracted(String extractedText, int selectedLanguageIndex);
	}

	
	/*=============================== Fragment Lifecycle functions ============================*/
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {

		// Retrieve state variables
		if (savedInstanceState!=null){
			mImageAvailableOnPath = savedInstanceState.getBoolean(Constants.STATE_IMAGE_AVAILABLE_ON_PATH);
			sSelectedLanguageIndex = savedInstanceState.getInt(Constants.STATE_SELECTED_LANGUAGE_INDEX);
			mTempImagePath = savedInstanceState.getString(Constants.STATE_TEMP_IMAGE_PATH);
		}else{
			// Load user location and country
			new LoadLocationBasedLanguageTask().execute(getActivity());
		}
		
		// Initialize the current path vatiable.
		File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
		mCurrentImagePath = storageDir.getAbsolutePath()+"/"+Constants.IMAGE_FILENAME_CURRENT+".jpg";
		
        View view = inflater.inflate(R.layout.fragment_photo, container, false);
        setHasOptionsMenu(true);

        setupView(view);

        return view;
    }

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.photo, menu);
		getActivity().getActionBar().setTitle(R.string.title_step_1);
		getActivity().getActionBar().setSubtitle(R.string.title_capture);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(Constants.STATE_IMAGE_AVAILABLE_ON_PATH, mImageAvailableOnPath);
		outState.putInt(Constants.STATE_SELECTED_LANGUAGE_INDEX, sSelectedLanguageIndex);
		outState.putString(Constants.STATE_TEMP_IMAGE_PATH, mTempImagePath);
	}

	@Override
	public void onResume() {
		super.onResume();
	
		// When the fragment is resumed, check if any async tasks have returned while the
		// fragment was paused
		SharedPreferences prefs = getActivity().getSharedPreferences(
				Constants.SHARED_PREF_OCR_DIALOG, 0);
		Boolean serverReturned = prefs.getBoolean(Constants.SP_KEY_SERVER_RETURNED_WHILE_AWAY, false);
		
		// If there is data returned by the async task, process it.
		if (serverReturned){
			String result = prefs.getString(Constants.SP_KEY_RESULT, null);
			String exMessage = prefs.getString(Constants.SP_KEY_EXCEPTION, null);
			SharedPreferences.Editor editor = prefs.edit();
			editor.putBoolean(Constants.SP_KEY_SERVER_RETURNED_WHILE_AWAY, false);
			editor.commit();
			processResultString(result, getActivity(), exMessage);
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (!getActivity().isChangingConfigurations()){
			// Destroy any saved images
			if (mCurrentImagePath!=null){
				new File(mCurrentImagePath).delete();
			}
			if (mTempImagePath!=null){
				new File(mTempImagePath).delete();
			}
			
			// Delete all shared preferences
			SharedPreferences prefs = getActivity().getSharedPreferences(Constants.SHARED_PREF_OCR_DIALOG, 0);
			prefs.edit().clear().commit();
			
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {

		// The camera has returned
		case REQUEST_TAKE_PHOTO:
			switch (resultCode) {
			case Activity.RESULT_OK:
				
				// Copy the image from temp path to current path.
				File currentImageFile = new File(mCurrentImagePath);
				File tempImageFile = new File(mTempImagePath);
				FileUtils.overwriteOneFileWithAnother(tempImageFile, currentImageFile);
				
				// delete the temp file
				tempImageFile.delete();
			    tempImageFile=null;
			    
				mImageAvailableOnPath=true;

				// Show the image inside the fragment.
				showCurrentImage();
				
				break;
				
			case Activity.RESULT_CANCELED:
				// Delete the file on temp path and simply return
				if (mTempImagePath!=null){
					new File(mTempImagePath).delete();
				}
				break;
			}
			break;
		
		// The edited image has already been saved on current path. Simply show it in the
		// fragment.
		case REQUEST_EDIT_PHOTO:
			switch (resultCode) {
			case Activity.RESULT_OK:
			case Activity.RESULT_CANCELED:
				showCurrentImage();
				break;
			}
			break;
		}
	}

	
	/*========================================= Helper functions ==================================*/
	
	/**
	 * Sets up all the views
	 * @param view
	 */
	private void setupView(View view){
		
		// Set up default layout elements
		mTapCameraMessageTextView = (TextView) view.findViewById(R.id.tap_camera);
		mTapCameraMessageTextView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				invokeCamera(v);
			}
		});
		mPhotoFragmentRootLayout = (RelativeLayout) view.findViewById(R.id.photo_view);

		mButton1 = (Button) getActivity().findViewById(R.id.button1);
        mButton2 = (Button) getActivity().findViewById(R.id.button2);
        
        // Change the layout as per needs/state
        if (mImageAvailableOnPath){
        	showCurrentImage();
        }
	}
	
	/**
	 * Disables Tap message and shows the image that us stored on pathToImageFile
	 * @param pathToImageFile
	 */
	private void showCurrentImage(){
		
		// If photo is not already visible, add the corresponding view to the viewgroup
		if (mPhotoImageView==null){
			mPhotoFragmentRootLayout.removeView(mTapCameraMessageTextView);
						
			View view = LayoutInflater.from(getActivity()).inflate(R.layout.view_photo_display, null);
			RelativeLayout rl = (RelativeLayout) view.findViewById(R.id.rel_layout);
			mPhotoImageView = (ImageView) view.findViewById(R.id.photo_image_view);
			mPhotoFragmentRootLayout.addView(rl);
        }
		
		// Load a scaled down version of image and show it
		Point size = new Point();
		getActivity().getWindowManager().getDefaultDisplay().getSize(size);
	    Bitmap bitmap = BitmapUtils.loadImage(
	    		mCurrentImagePath, 
	    		BitmapUtils.ImageLoadMode.LOAD_FOR_VIEW,
	    		size);
	   
	    mPhotoImageView.setImageBitmap(bitmap);
	    
	    // Enable the buttons to use the image
	    mButton1.setEnabled(true);
	    mButton2.setEnabled(true);
	}
	
	/**
	 * Processes the extracted text returned from the OCR, and calls appropriate listener. 
	 * @param result String returned by OCR
	 * @param context Context of the application
	 * @param exMessage Any Exception string
	 */
	private static void processResultString(String result, Context context, String exMessage){
		if (result==null){
			// The result is null, check for exception.
    		if (exMessage.equals("server timed out")){
    			Toast.makeText(context, R.string.notif_server_unavailable, 
    					Toast.LENGTH_LONG).show();
    		}else if (exMessage.equals("some other exception")){
    			Toast.makeText(context, R.string.notif_unknown_error, 
    					Toast.LENGTH_LONG).show();
    		}
		}else if (result.isEmpty()){
			Toast.makeText(context, R.string.notif_nothing_extracted, 
					Toast.LENGTH_LONG).show();
		}else{
    		((PhotoFragmentListener)context).onTextExtracted(result.trim(),sSelectedLanguageIndex);
    	}
	}
	

	/*============================= Callback and listener functions ==============================*/

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_capture:
			invokeCamera(null);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	/**
	 * Sets up temporary image files to store results and then invokes camera activity.
	 * @param view
	 */
	public void invokeCamera(View view){
		
		// Set intent for camera
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
	
		// If a camera activity is present, proceed with the next steps.
		if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
	        
	        File photoFile = null;
	        try {
	        	// Prepare a temp file to store camera result 
	        	File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
	            photoFile = FileUtils.createTempImageFile(storageDir);
	            mTempImagePath = photoFile.getAbsolutePath();
	        } catch (IOException ex) {
	        	ex.printStackTrace();
	        	return;
	        }
	        if (photoFile != null) {
	        	// Invoke the camera activity
	            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
	            startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
	        }
	    }
	}

	@Override
	public void onOCRLanguageDialogPositiveClick(int selectedItemIndex) {
		
		// Set user-selected language index
		sSelectedLanguageIndex = selectedItemIndex;
		
		// Choose a language code that will run on remote OCR
		String ocrCode = getResources().getStringArray(R.array.supported_languages_ocr_codes)[selectedItemIndex];
		
		// Once the dialog is closed, load the bitmap and run OCR
		Bitmap bMap = BitmapUtils.loadImage(
				mCurrentImagePath, 
				BitmapUtils.ImageLoadMode.LOAD_FOR_OCR,
				null);
		new RunOCRTask(getActivity()).execute(
							bMap, 
							ocrCode);
	}

	@Override
	public void onPageSelected() {
		
		// Update button listeners and corresponding text. These buttons are contained in the
		// parent holder activity and need to be updated for each fragment.
		mButton1.setText(R.string.action_edit_image);
		mButton1.setEnabled(mImageAvailableOnPath);
        mButton1.setOnClickListener(new OnClickListener() {	
			@Override
			public void onClick(View v) {
				
				// Start edit photo activity
				Intent i = new Intent(getActivity(),SimpleImageEditor.class);
				i.putExtra(SimpleImageEditor.IMAGE_PATH, mCurrentImagePath);
				startActivityForResult(i, REQUEST_EDIT_PHOTO);
			}
		});
		
        
		mButton2.setText(R.string.action_run_ocr);
		mButton2.setEnabled(mImageAvailableOnPath);
        mButton2.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ConnectivityManager connMgr = (ConnectivityManager) 
			            getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
				if (Controller.testInternetConnection(connMgr)){
					
					// Show the language picker dialog when this button is pressed.
					DialogFragment dialog =  new OCRLanguagePickerDialogFragment();
					Bundle args = new Bundle();
					args.putInt(Constants.ARG_SELECTED_LANGUAGE_INDEX, sSelectedLanguageIndex);
					dialog.setArguments(args);
					dialog.show(getFragmentManager(), Constants.TAG_OCR_LANGUAGE_DIALOG);
				}else{
					Toast.makeText(getActivity(), R.string.notif_no_internet, Toast.LENGTH_SHORT).show();
				}
			}
		});
	}
	

	
	/*======================================== Async Tasks ========================================*/
	
	/**
	 * Asynctask that queries for location.
	 * @author Fouad
	 */
	private static class LoadLocationBasedLanguageTask 
						extends AsyncTask<Activity, Void, String>{
		
		private Activity mActivity;

		@Override
		protected String doInBackground(Activity... params) {
			// get user location and country
			mActivity = params[0];
			LocationManager locationManager = (LocationManager)
					mActivity.getSystemService(Context.LOCATION_SERVICE);
			Geocoder geocoder = new Geocoder(params[0]);
			String country = Controller.getLocation(
					geocoder, locationManager);
			
			return country;
		}

		@Override
		protected void onPostExecute(String country) {
			super.onPostExecute(country);
			// Get user language from the country. If the GPS fails to return a country,
			// silently exit the task and set English as the language.
			if (country != null){
				String[] supportedCountries = mActivity.getResources().getStringArray(R.array.supported_countries);
				for (int i=0; i<supportedCountries.length;i++){
					if (supportedCountries[i].toLowerCase().equals(country.toLowerCase())){
						sSelectedLanguageIndex = i;
						break;
					}
				}
			}
		}
    }
	
	/**
	 * Custom OCR task that runs in background while waiting for the server to respond. 
	 * @author Fouad
	 */
	private static class RunOCRTask
						extends CustomAsyncTask<Object, Void, String>{
		private ProgressDialog 		mProgressDialog;
		private Exception 			mException;
		private Timer 				mTimer;
		private int 				mTimeElapsedS;
		private SharedPreferences 	mPrefs;
		private ConnectionHandler 	mConnection;
		
		public RunOCRTask(Activity activity){
			super(activity);
		}
		
		@Override
        protected void onPreExecute() {
			// Initialize the variables
            super.onPreExecute();
            showProgressDialog();
            mTimeElapsedS=0;
            mTimer = new Timer();
            mPrefs = mActivity.getSharedPreferences(Constants.SHARED_PREF_OCR_DIALOG, 0);
        }
		
		/**
		 * Set up and show the progress dialog
		 */
		private void showProgressDialog(){
			mProgressDialog = new ProgressDialog(mActivity);
	    	mProgressDialog.setTitle(R.string.title_running_ocr);
	    	mProgressDialog.setMessage(mActivity.getResources().getString(R.string.notif_running_ocr)+" "+mTimeElapsedS+"s");
	    	mProgressDialog.setCancelable(true);
	    	mProgressDialog.setIndeterminate(true);
	    	mProgressDialog.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					mTimer.cancel();
					mProgressDialog.dismiss();
					// Request connection to be terminated when the dialog is cancelled.
					mConnection.terminateConnection();
					RunOCRTask.this.cancel(true);
				}
			});
	    	mProgressDialog.show();
		}
 
        @Override
        protected void onActivityDetached() {
        	
        	// Dismiss the dialog when activity is detached.
            if (mProgressDialog != null) {
               mProgressDialog.dismiss();
               mProgressDialog = null;
            }
        }
 
        @Override
        protected void onActivityAttached() {
        	// Create the new dialog when activity is recreated and attached.
        	if (!isCancelled())
        		showProgressDialog();
        }
		
		@Override
		protected String doInBackground(Object... params) {
			try {
				// Retrieve the parameters
	    		Bitmap bMap = (Bitmap) params[0];
	    		String language = (String) params[1];
	    		
	    		// Start a counter timer
	    	    mTimer.scheduleAtFixedRate (new TimerTask() {
					@Override
					public void run() {
						mTimeElapsedS+=1;
	                    RunOCRTask.this.publishProgress();
					}
				}, 0, 1000);
	    	    
	    	    // Create an instance of connection handler.
	    	    mConnection = new ConnectionHandler();
	    	    
	    	    // Run OCR and return
				return Controller.runOCR(bMap,language, mConnection);
			}catch (SocketTimeoutException e){
				e.printStackTrace();
				mException = new Exception("server timed out");
				return null;
			}catch (Exception e) {
				e.printStackTrace();
				mException = new Exception("some other exception");
				return null;
			}
		}
		
		@Override
		protected void onProgressUpdate(Void... values) {
			
			// Update the dialog message every second
			if (mActivity!=null){
				mProgressDialog.setMessage(mActivity.getResources().
						getString(R.string.notif_running_ocr)+" "+mTimeElapsedS+"s");
			}
		}

		@Override
	    protected void onPostExecute(String result) {
			super.onPostExecute(result);
			
			// Cancel the timer
			mTimer.cancel();
			
			if (mActivity != null) {
				
				// If the activity is attached, cancel the dialog and process the returned result.
				mProgressDialog.dismiss();
				String exMessage = (mException==null) ? null : mException.getMessage();
				processResultString(result, mActivity, exMessage);
				
			}else{
				
				// If the activity is not attached, save the results to the disk so that when
				// the application gains focus again, the results could be used.
				SharedPreferences.Editor editor = mPrefs.edit();
				editor.putBoolean(Constants.SP_KEY_SERVER_RETURNED_WHILE_AWAY, true);
				editor.putString(Constants.SP_KEY_RESULT, result);
				String exMessage = (mException==null) ? null : mException.getMessage();
				editor.putString(Constants.SP_KEY_EXCEPTION, exMessage);
				editor.commit();
			}
	    }
		
		@Override
		protected void onCancelled(String result) {
			super.onCancelled(result);
		}
	}
}

