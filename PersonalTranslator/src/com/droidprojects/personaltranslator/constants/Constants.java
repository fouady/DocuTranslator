package com.droidprojects.personaltranslator.constants;

/**
 * The class contains all the constants used all over the project.
 * @author Fouad
 */
public class Constants {
	
	// URL of server where OCR resides
	public static final String 	SERVER_URL_OCR 				= "http://ocr-server.herokuapp.com/tesseract_pages/run";
	// URL of Bing translator server
	public static final String 	SERVER_URL_TRANSLATOR_BING	= "https://api.datamarket.azure.com/Bing/MicrosoftTranslator/v1/Translate";
	// Bing translator authentication key
	public static final String 	SERVER_PRIMARY_KEY_BING 	= "NeALIc+MXgtuxGtGXkYasQ94sXxwNEdQ5EynS94kN9I";
	// Max size of the larger dimension of image for OCR
	public static final int 	OCR_IMAGE_SMALLER_DIM 		= 2048;
	// filename of the image where current image is stored
	public static final String 	IMAGE_FILENAME_CURRENT 		= "JPG_PERSONAL_TRANSLATOR_CURRENT";
	
	
	// Shared Preferences names
	public static final String 	SHARED_PREF_OCR_DIALOG 			= "shared_pref_1";
	public static final String 	SHARED_PREF_TRANSLATOR_DIALOG 	= "shared_pref_2";
	public static final String 	SHARED_PREF_MAIN_ACTIVITY 		= "shared_pref_3";
	
	// Shared Preferences keys
	public static final String 	SP_KEY_SERVER_RETURNED_WHILE_AWAY 	= "shared_pref_value_1";
	public static final String 	SP_KEY_RESULT 						= "shared_pref_value_2";
	public static final String 	SP_KEY_EXCEPTION 					= "shared_pref_value_3";
	public static final String 	SP_KEY_SESSION_ID 					= "shared_pref_value_4";
	
	// Dialog Tags
	public static final String 	TAG_OCR_LANGUAGE_DIALOG 		= "dialog_1";
	public static final String 	TAG_TRANSLATOR_LANGUAGE_DIALOG	= "dialog_2";
	
	// keys for saved instance states
	// main activity
	public static final String 	STATE_CURRENT_PAGE_NUMBER 		= "state_key_1";
	// photo fragment
	public static final String 	STATE_CURRENT_IMAGE_PATH 		= "state_key_11";
	public static final String 	STATE_IMAGE_AVAILABLE_ON_PATH 	= "state_key_12";
	public static final String 	STATE_SELECTED_LANGUAGE_INDEX 	= "state_key_13";
	public static final String 	STATE_TEMP_IMAGE_PATH 			= "state_key_14";
	// ocr fragment
	public static final String 	STATE_FROM_LANGUAGE_INDEX 		= "state_key_21";
	public static final String 	STATE_TO_LANGUAGE_INDEX 		= "state_key_22";
	
	// keys for transfer arguments
	public static final String 	ARG_SELECTED_LANGUAGE_INDEX 	= "arg_key_1";
	public static final String 	ARG_FROM_LANGUAGE_INDEX 		= "arg_key_2";
	public static final String 	ARG_TO_LANGUAGE_INDEX 			= "arg_key_3";
}