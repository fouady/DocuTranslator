package com.droidprojects.personaltranslator.views;

import java.net.SocketTimeoutException;
import java.util.Timer;
import java.util.TimerTask;

import com.droidprojects.personaltranslator.R;
import com.droidprojects.personaltranslator.constants.Constants;
import com.droidprojects.personaltranslator.controllers.ConnectionHandler;
import com.droidprojects.personaltranslator.controllers.Controller;
import com.droidprojects.personaltranslator.customclasses.CustomAsyncTask;
import com.droidprojects.personaltranslator.views.FragmentHolderActivity.FragmentHolderListener;
import com.droidprojects.personaltranslator.views.TranslatorLanguagePickerDialogFragment.TranslatorLanguagePickerDialogListener;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The purpose of this Fragment is to show the extracted text to user obtained after running
 * the OCR. The user can modify the text and then run the translator on it. 
 * @author Fouad
 */
public class OCRFragment extends Fragment implements 
									FragmentHolderListener, 
									TranslatorLanguagePickerDialogListener {

	// Views from the layout
	private EditText 	mExtractedTextHolderEditText;	// Holds text returned by OCR
	private TextView 	mExtractedTextHeaderTextView;	// Holds heading
	private Button 		mButton1;						// Reference to first button in parent activity
	private Button 		mButton2;						// Reference to second button in parent activity

	// state variables
	private int mFromLanguageIndex = 0;	// Language of the text
	private int mToLanguageIndex = 0;	// Language to which it has to be translated
	
	/**
	 * The listener implementing this interface will be notified when the text has been translated.
	 * @author Fouad
	 */
	public interface OCRFragmentListener{
		/**
		 * Called when text has been translated.
		 * @param translatedText
		 */
		public void onTextTranslated(String translatedText);
	}

	
	/*=================================== Fragment lifecycle functions =============================*/
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		// Retrieve any state variables
		if (savedInstanceState!=null){
			mFromLanguageIndex = savedInstanceState.getInt(Constants.STATE_FROM_LANGUAGE_INDEX);
			mToLanguageIndex = savedInstanceState.getInt(Constants.STATE_TO_LANGUAGE_INDEX);
		}
		
		View view = inflater.inflate(R.layout.fragment_ocr, container, false);
		setHasOptionsMenu(true);

		setupView(view);

		return view;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		getActivity().getActionBar().setTitle(R.string.title_step_2);
		getActivity().getActionBar().setSubtitle(R.string.title_extracted_text);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		// Save state variables
		super.onSaveInstanceState(outState);
		outState.putInt(Constants.STATE_FROM_LANGUAGE_INDEX, mFromLanguageIndex);
		outState.putInt(Constants.STATE_TO_LANGUAGE_INDEX, mToLanguageIndex);
	}

	@Override
	public void onResume() {
		super.onResume();
	
		// When the fragment is resumed, check if any async tasks have returned while the
		// fragment was paused
		SharedPreferences prefs = getActivity().getSharedPreferences(
				Constants.SHARED_PREF_TRANSLATOR_DIALOG, 0);
		
		// If there is data returned by the async task, process it.
		Boolean serverReturned = prefs.getBoolean(Constants.SP_KEY_SERVER_RETURNED_WHILE_AWAY, false);
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
			// Delete all shared preferences
			SharedPreferences prefs = getActivity().getSharedPreferences(Constants.SHARED_PREF_TRANSLATOR_DIALOG, 0);
			prefs.edit().clear().commit();
		}
	}
	
	
	/*================================= Helper functions =========================================*/
	
	private void setupView(View view) {
		
		// setup the layout elements
		mExtractedTextHolderEditText = (EditText) view.findViewById(R.id.editText_extracted_text);

		mExtractedTextHeaderTextView = (TextView) view.findViewById(R.id.textView_extracted_header);
		mExtractedTextHeaderTextView.setText(Html.fromHtml(getResources().getString(R.string.notif_extracted_text)));

		mButton1 = (Button) getActivity().findViewById(R.id.button1);
		mButton2 = (Button) getActivity().findViewById(R.id.button2);
	}

	/**
	 * This function is called by the parent activity when this fragment becomes visible
	 * to the user. It ensures that this fragment has the updated data/information
	 * @param extractedText Text extracted by the OCR
	 * @param selectedLanguageIndex The language of the text
	 */
	public void showOCRFragment(String extractedText, int selectedLanguageIndex) {
		this.mFromLanguageIndex = selectedLanguageIndex;
		mExtractedTextHolderEditText.setText(extractedText);
	}
	
	/**
	 * Processes the translated text returned from the translator, and calls appropriate listener. 
	 * @param result String returned by translator
	 * @param context Context of the application
	 * @param exMessage Any Exception string
	 */
	private static void processResultString(String result, Context context, String exMessage){
		if (result==null){
    		if (exMessage.equals("server timed out")){
    			Toast.makeText(context, R.string.notif_server_unavailable, 
    					Toast.LENGTH_LONG).show();
    		}else if (exMessage.equals("some other exception")){
    			Toast.makeText(context, R.string.notif_unknown_error, 
    					Toast.LENGTH_LONG).show();
    		}
		}else{
    		((OCRFragmentListener)context).onTextTranslated(result.trim());
    	}
	}
	
	
	/*============================== Callback and listener functions ==============================*/
	
	@Override
	public void onPageSelected() {
		
		// Update button listeners and corresponding text. These buttons are contained in the
		// parent holder activity and need to be updated for each fragment.
		mButton1.setText(R.string.action_back);
		mButton1.setEnabled(true);
		mButton1.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Go to previous fragment
				getActivity().onBackPressed();
			}
		});

		mButton2.setText(R.string.action_run_translator);
		mButton2.setEnabled(true);
		mButton2.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// show the language picker dialog
				ConnectivityManager connMgr = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
				if (Controller.testInternetConnection(connMgr)) {
					DialogFragment dialog = new TranslatorLanguagePickerDialogFragment();
					Bundle args = new Bundle();
					args.putInt(Constants.ARG_FROM_LANGUAGE_INDEX, mFromLanguageIndex);
					args.putInt(Constants.ARG_TO_LANGUAGE_INDEX, mToLanguageIndex);
					dialog.setArguments(args);
					dialog.show(getFragmentManager(), Constants.TAG_TRANSLATOR_LANGUAGE_DIALOG);
				} else {
					Toast.makeText(getActivity(), R.string.notif_no_internet, Toast.LENGTH_SHORT).show();
				}
			}
		});
	}

	@Override
	public void onTranslsatorLanguageDialogPositiveClick(int fromLanguageIndex, int toLanguageIndex) {
		
		// Retrieve list of all supported languages
		String[] allTranslatorLanguages = getResources().getStringArray(R.array.supported_languages_bing); 
		
		// Get the appropriate languages for the translator
		String fromLanguage = allTranslatorLanguages[fromLanguageIndex];
		String toLanguage = allTranslatorLanguages[toLanguageIndex];
		
		// Run the Translator task
		new RunTranslatorTask(getActivity()).execute(
				mExtractedTextHolderEditText.getText().toString(),
				fromLanguage,
				toLanguage);
	}
	
	
	/*============================== Async tasks =================================================*/
	
	/**
	 * Custom Translator task that runs in background while waiting for the server to respond. 
	 * @author Fouad
	 */
	private static class RunTranslatorTask extends CustomAsyncTask<String, Void, String> {
		private ProgressDialog 		mProgressDialog;
		private Exception 			mException;
		private Timer 				mTimer;
		private int 				mTimeElapsedS;
		private SharedPreferences 	mPrefs;
		private ConnectionHandler 	mConnection;

		public RunTranslatorTask(Activity activity) {
			super(activity);
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			// Initialize the variables
			showProgressDialog();
			mTimeElapsedS = 0;
			mTimer = new Timer();
			mPrefs = mActivity.getSharedPreferences(Constants.SHARED_PREF_TRANSLATOR_DIALOG, 0);
		}

		private void showProgressDialog() {
			mProgressDialog = new ProgressDialog(mActivity);
			mProgressDialog.setTitle(R.string.title_running_translator);
			mProgressDialog.setMessage(mActivity.getResources().getString(R.string.notif_running_translator) + " "+mTimeElapsedS + "s");
			mProgressDialog.setCancelable(true);
			mProgressDialog.setIndeterminate(true);
			mProgressDialog.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					mTimer.cancel();
					mProgressDialog.dismiss();
					// Request connection to be terminated when the dialog is cancelled.
					mConnection.terminateConnection();
					RunTranslatorTask.this.cancel(true);
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
		protected String doInBackground(String... params) {
			try {
				// Retrieve parameters
				String text = params[0];
				String fromLanguage = params[1];
				String toLanguage = params[2];
				
				// Start counter timer
				mTimer.scheduleAtFixedRate(new TimerTask() {
					@Override
					public void run() {
						mTimeElapsedS += 1;
						RunTranslatorTask.this.publishProgress();
					}
				}, 0, 1000);
				
				// Create an instance of connection handler.
	    	    mConnection = new ConnectionHandler();
				
	    	    // Run translator and return
				return Controller.runTranslator(text, fromLanguage, toLanguage);
			} catch (SocketTimeoutException e) {
				e.printStackTrace();
				mException = new Exception("server timed out");
				return null;
			} catch (Exception e) {
				e.printStackTrace();
				mException = new Exception("some other exception");
				return null;
			}
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			
			// Update the dialog message every second
			if (mActivity != null) {
				mProgressDialog.setMessage(mActivity.getResources().getString(R.string.notif_running_translator) + " "+mTimeElapsedS + "s");
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
				String exMessage = (mException == null) ? null : mException.getMessage();
				processResultString(result, mActivity, exMessage);
			} else {
				
				// If the activity is not attached, save the results to the disk so that when
				// the application gains focus again, the results could be used.
				SharedPreferences.Editor editor = mPrefs.edit();
				editor.putBoolean(Constants.SP_KEY_SERVER_RETURNED_WHILE_AWAY, true);
				editor.putString(Constants.SP_KEY_RESULT, result);
				String exMessage = (mException == null) ? null : mException.getMessage();
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