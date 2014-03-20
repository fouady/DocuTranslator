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
 * The dialog that allows the users to pick a language which will be used to extract
 * the text from the image.
 * @author Fouad
 */
public class OCRLanguagePickerDialogFragment extends DialogFragment{

	private OCRLanguagePickerDialogListener mListener;

	/**
	 * Listener that implements this interface will be notified when the dialog exists.
	 * @author Fouad
	 */
	public interface OCRLanguagePickerDialogListener{
		/**
		 * The function is called when OK is pressed
		 * @param seletedItemIndex The index of selected language
		 */
		public void onOCRLanguageDialogPositiveClick(int seletedItemIndex);
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		// Inflate with the layout
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View view = inflater.inflate(R.layout.dialog_ocr_language_selector, null);
		
		// Extract language index
		int selectedLanuageIndex = getArguments().getInt(Constants.ARG_SELECTED_LANGUAGE_INDEX); 
		
		// Set up spinner and corresponding adapter
		final Spinner spinner = (Spinner) view.findViewById(R.id.spinner_languages_ocr);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
				R.array.supported_languages_ocr, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setSelection(selectedLanuageIndex, true);
		
		// Create the dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setView(view)
			.setTitle(R.string.title_language_selector)
			.setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					int selectedItemIndex = spinner.getSelectedItemPosition();
					mListener.onOCRLanguageDialogPositiveClick(selectedItemIndex);
				}
			});
		
		return builder.create();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			// Store the reference to the listener activity
			mListener=(OCRLanguagePickerDialogListener) activity;
		}catch (ClassCastException ex){
			throw new ClassCastException(activity.toString() + 
					" must implement OCRLanguagePickerDialogListener");
		}
	}
}
