package com.droidprojects.personaltranslator.views;


import com.droidprojects.personaltranslator.R;
import com.droidprojects.personaltranslator.constants.Constants;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

/**
 * The dialog that allows the user to select languages for translation
 * @author Fouad
 */
public class TranslatorLanguagePickerDialogFragment extends DialogFragment {

	private TranslatorLanguagePickerDialogListener mListener;
	
	/**
	 * The listener implementing this interface is notified when the dialog exits
	 * @author Fouad
	 */
	public interface TranslatorLanguagePickerDialogListener {
		/**
		 * The function is called when OK is clicked. 
		 * @param fromLanguageIndex The language from which text will be translated
		 * @param toLanguageIndex The language to which text will be translated.
		 */
		public void onTranslsatorLanguageDialogPositiveClick(int fromLanguageIndex, int toLanguageIndex);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		// Inflate with the layout
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View view = inflater.inflate(R.layout.dialog_translator_language_selector, null);

		// Extract language index
		int fromLanuageIndex = getArguments().getInt(Constants.ARG_FROM_LANGUAGE_INDEX);
		int toLanuageIndex = getArguments().getInt(Constants.ARG_TO_LANGUAGE_INDEX);

		// Set up spinner and corresponding adapter - from language
		final Spinner spinnerFrom = (Spinner) view.findViewById(R.id.spinner_languages_translator_from);
		ArrayAdapter<CharSequence> adapterFrom = ArrayAdapter.createFromResource(getActivity(), R.array.supported_languages_ocr, android.R.layout.simple_spinner_item);
		adapterFrom.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerFrom.setAdapter(adapterFrom);
		spinnerFrom.setSelection(fromLanuageIndex, true);

		// Set up spinner and corresponding adapter - to language
		final Spinner spinnerTo = (Spinner) view.findViewById(R.id.spinner_languages_translator_to);
		ArrayAdapter<CharSequence> adapterTo = ArrayAdapter.createFromResource(getActivity(), R.array.supported_languages_ocr, android.R.layout.simple_spinner_item);
		adapterTo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerTo.setAdapter(adapterTo);
		spinnerTo.setSelection(toLanuageIndex, true);

		// Create the dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setView(view).setTitle(R.string.title_language_selector).setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				int fromLanguageIndex = spinnerFrom.getSelectedItemPosition();
				int toLanguageIndex = spinnerTo.getSelectedItemPosition();
				mListener.onTranslsatorLanguageDialogPositiveClick(fromLanguageIndex, toLanguageIndex);
			}
		});
		return builder.create();
	}

	

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			// Store the activity as listener
			mListener = (TranslatorLanguagePickerDialogListener) activity;
		} catch (ClassCastException ex) {
			throw new ClassCastException(activity.toString() + " must implement TranslatorLanguagePickerDialogListener");
		}
	}
}
