package com.droidprojects.personaltranslator.views;

import com.droidprojects.personaltranslator.R;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

/**
 * Activity that shows scanning tips.
 * @author Fouad
 */
public class ScanningTipsActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_scanning_tips);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return false;
	}

}
