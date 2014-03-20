package com.droidprojects.personaltranslator.views;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.droidprojects.personaltranslator.R;
import com.droidprojects.personaltranslator.views.FragmentHolderActivity.FragmentHolderListener;

public class TranslatorFragment extends Fragment
								implements FragmentHolderListener{
	// views from the layout
	private Button 		mButton1;
	private Button 		mButton2;
	private EditText	mTranslatedTextHolderEditText;
	private TextView 	mTranslatedTextHeaderTextView;
	
	// listeners
	private TranslatorFragmentListener mListener;
	
	public interface TranslatorFragmentListener{
		public void onStartButtonPressed();
	}
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
		
		View view = inflater.inflate(R.layout.fragment_translator, container, false);
		setHasOptionsMenu(true);
		
		setupView(view);
		
        return view;
    }
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.main, menu);
		getActivity().getActionBar().setTitle(R.string.title_step_3);
		getActivity().getActionBar().setSubtitle(R.string.title_translated_text);
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try{
			mListener = (TranslatorFragmentListener) activity;
		}catch (ClassCastException ex){
			throw new ClassCastException(activity.toString() + 
					" must implement TranslatorFragmentListener");
		}
	}
	
	private void setupView(View view){
		mTranslatedTextHolderEditText = (EditText) view.findViewById(R.id.editText_translated_text);

		mTranslatedTextHeaderTextView = (TextView) view.findViewById(R.id.textView_translated_header);
		mTranslatedTextHeaderTextView.setText(Html.fromHtml(getResources().getString(R.string.notif_translated_text)));

		mButton1 = (Button) getActivity().findViewById(R.id.button1);
		mButton2 = (Button) getActivity().findViewById(R.id.button2);
	}
	
	public void showTranslatorFragment(String translatedText) {
		mTranslatedTextHolderEditText.setText(translatedText);
	}
	
	// callbacks and listeners
	@Override
	public void onPageSelected() {
		mButton1.setText(R.string.action_back);
		mButton1.setEnabled(true);
		mButton1.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				getActivity().onBackPressed();
			}
		});
		
		mButton2.setText(R.string.action_start_again);
		mButton2.setEnabled(true);
		mButton2.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mListener.onStartButtonPressed();
			}
		});	
	}
}
