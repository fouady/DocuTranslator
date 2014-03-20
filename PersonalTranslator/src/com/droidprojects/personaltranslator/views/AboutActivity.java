package com.droidprojects.personaltranslator.views;

import com.droidprojects.personaltranslator.R;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

/**
 * Shows the About text for the app
 * @author Fouad
 */
public class AboutActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return false;
	}

}
