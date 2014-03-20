package com.droidprojects.personaltranslator.views;

import java.util.HashMap;

import com.droidprojects.personaltranslator.R;
import com.droidprojects.personaltranslator.constants.Constants;
import com.droidprojects.personaltranslator.customclasses.AsynctaskSupportedApplication;
import com.droidprojects.personaltranslator.views.OCRFragment.OCRFragmentListener;
import com.droidprojects.personaltranslator.views.OCRLanguagePickerDialogFragment.OCRLanguagePickerDialogListener;
import com.droidprojects.personaltranslator.views.PhotoFragment.PhotoFragmentListener;
import com.droidprojects.personaltranslator.views.TranslatorFragment.TranslatorFragmentListener;
import com.droidprojects.personaltranslator.views.TranslatorLanguagePickerDialogFragment.TranslatorLanguagePickerDialogListener;

import android.os.Bundle;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;

/**
 * This is the parent activity that holds the viewpager (which holds the fragments).
 * It also listens to the child views and passes the message on to the appropriate
 * views and fragments.
 * @author Fouad
 */
public class FragmentHolderActivity extends FragmentActivity implements 
											OCRLanguagePickerDialogListener, 
											TranslatorLanguagePickerDialogListener, 
											PhotoFragmentListener, 
											OCRFragmentListener, 
											TranslatorFragmentListener {

	// Viewgroups and Activity specific objects
	private ViewPager 		mViewPager;
	private PagerAdapter 	mPagerAdapter;

	// Pages hashmap
	private HashMap<Integer, Fragment> pageFragments;

	// Page indices
	private static final int PAGE_PHOTO_FRAGMENT 		= 0;
	private static final int PAGE_OCR_FRAGMENT 			= 1;
	private static final int PAGE_TRANSLATOR_FRAGMENT 	= 2;
	private static final int NUM_PAGES 					= 3;

	// state variables
	private int mCurrentPage;

	/**
	 * The classes implementing this listener are notified when a new page is selected.
	 * @author Fouad
	 */
	public interface FragmentHolderListener {
		public void onPageSelected();
	}
	
	
	/*========================= Activity LifeCycle Functions ========================*/
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Load instance variables
		if (savedInstanceState != null) {
			mCurrentPage = savedInstanceState.getInt(Constants.STATE_CURRENT_PAGE_NUMBER);
		} else {
			mCurrentPage = PAGE_PHOTO_FRAGMENT;
		}

		// Initialise hashmap
		pageFragments = new HashMap<Integer, Fragment>(NUM_PAGES);

		setupFragments();
		checkSession();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main, menu);
	    return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		// Detach the activity when app is paused. This saves any async tasks.
		((AsynctaskSupportedApplication) getApplication()).detach(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		// Attach the activity when the app is resumed. This restored any async tasks.
		((AsynctaskSupportedApplication) getApplication()).attach(this);
		
		// Schedule to select the current page in the queue. This is because the viewpager might not
		// have been fully initialized by now.
		mViewPager.post(new Runnable() {
			@Override
			public void run() {
				((FragmentHolderListener) pageFragments.get(mCurrentPage)).onPageSelected();
			}
		});
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		// Save the state variables
		outState.putInt(Constants.STATE_CURRENT_PAGE_NUMBER, mCurrentPage);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		// save session id on disk which will later determine the state of async tasks
		SharedPreferences prefs = getSharedPreferences(Constants.SHARED_PREF_MAIN_ACTIVITY, 0);
		String currentSessionID = ((AsynctaskSupportedApplication)getApplication()).getSessionID();
		prefs.edit().putString(Constants.SP_KEY_SESSION_ID, currentSessionID).commit();
	}
	
	
	/*========================== Helper functions ============================*/
	
	/**
	 * Sets up viewpager, pager adapter and the corresponding fragments.
	 */
	private void setupFragments(){
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mPagerAdapter = new ScreenSlidePagerAdapter(getFragmentManager());
		mViewPager.setAdapter(mPagerAdapter);
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				super.onPageSelected(position);
				mCurrentPage = position;

				// If the entry in hashmap is null, wait for the
				// viewAdapter to
				// instantiate the references to the fragments and store
				// them in
				// the hashmap. onPageSelected() would then be called
				// from within onResume()
				if (pageFragments.get(position) != null) {
					((FragmentHolderListener) pageFragments.get(position)).onPageSelected();
				}
			}

		});
		mViewPager.setOffscreenPageLimit(NUM_PAGES);
	}

	/**
	 * Checks if the session is same as before.
	 */
	private void checkSession(){
		
		// Get session id from disk
		SharedPreferences prefs = getSharedPreferences(Constants.SHARED_PREF_MAIN_ACTIVITY, 0);
		String previousSessionID = prefs.getString(Constants.SP_KEY_SESSION_ID, "none");
		String currentSessionID = ((AsynctaskSupportedApplication)getApplication()).getSessionID();
		
		// Compare current session id with the one retrieved from disk.
		if (!previousSessionID.equals(currentSessionID)){
			
			// If the sessions are different, destroy any stored shared preferences.
			// This will prevent the fragments from loading any async task related
			// data that was stored in a previous session.
			prefs = getSharedPreferences(Constants.SHARED_PREF_OCR_DIALOG, 0);
			prefs.edit().clear().commit();
			prefs = getSharedPreferences(Constants.SHARED_PREF_TRANSLATOR_DIALOG, 0);
			prefs.edit().clear().commit();
		}
	}

	/**
	 * Extends and overrides functions from Fragment StatePagerAdapter.
	 * @author Fouad
	 */
	private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

		public ScreenSlidePagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			Fragment page;
			switch (position) {
			case PAGE_PHOTO_FRAGMENT:
				page = new PhotoFragment();
				break;
			case PAGE_OCR_FRAGMENT:
				page = new OCRFragment();
				break;
			case PAGE_TRANSLATOR_FRAGMENT:
				page = new TranslatorFragment();
				break;
			default:
				return null;
			}
			return page;
		}

		@Override
		public int getCount() {
			return NUM_PAGES;
		}

		@Override
		public Object instantiateItem(ViewGroup arg0, int arg1) {
			Object page = super.instantiateItem(arg0, arg1);
			// At this point the activity has been (re)created and pages hashmap
			// needs to be filled up with the fragment instances again.
			// This is the right place to retrieve the fragments and store
			// their instances.
			if (arg1 < NUM_PAGES) {
				pageFragments.put(arg1, (Fragment) page);
			}
			return page;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_about:{
			Intent i = new Intent(this, AboutActivity.class);
			startActivity(i);
			return true;
		}
		case R.id.action_exit:
			finish();
			return true;
		case R.id.action_tips:{
			Intent i = new Intent(this, ScanningTipsActivity.class);
			startActivity(i);
			return true;
		}
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	
	/*============================ Callback and listener functions =============================*/
	
	@Override
	public void onBackPressed() {
		if (mCurrentPage > 0) {
			mCurrentPage--;
			mViewPager.setCurrentItem(mCurrentPage, true);
		} else {
			super.onBackPressed();
			finish();
		}
	}

	@Override
	public void onOCRLanguageDialogPositiveClick(int selectedItemIndex) {
		PhotoFragment photoFragment = (PhotoFragment) pageFragments.get(PAGE_PHOTO_FRAGMENT);
		photoFragment.onOCRLanguageDialogPositiveClick(selectedItemIndex);
	}

	@Override
	public void onTextExtracted(String extractedText, int selectedLanguageIndex) {
		OCRFragment ocrFragment = (OCRFragment) pageFragments.get(PAGE_OCR_FRAGMENT);
		ocrFragment.showOCRFragment(extractedText, selectedLanguageIndex);
		mViewPager.setCurrentItem(PAGE_OCR_FRAGMENT, true);
	}

	@Override
	public void onTranslsatorLanguageDialogPositiveClick(int fromLanguageIndex, int toLanguageIndex) {
		OCRFragment ocrFragment = (OCRFragment) pageFragments.get(PAGE_OCR_FRAGMENT);
		ocrFragment.onTranslsatorLanguageDialogPositiveClick(fromLanguageIndex, toLanguageIndex);
	}

	@Override
	public void onTextTranslated(String translatedText) {
		TranslatorFragment translatorFragment = (TranslatorFragment) pageFragments.get(PAGE_TRANSLATOR_FRAGMENT);
		translatorFragment.showTranslatorFragment(translatedText);
		mViewPager.setCurrentItem(PAGE_TRANSLATOR_FRAGMENT, true);
	}

	@Override
	public void onStartButtonPressed() {
		mViewPager.setCurrentItem(PAGE_PHOTO_FRAGMENT,true);
	}
}
