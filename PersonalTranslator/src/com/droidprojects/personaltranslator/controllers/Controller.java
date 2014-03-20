package com.droidprojects.personaltranslator.controllers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import android.graphics.Bitmap;
import android.location.Geocoder;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Base64;

/**
 * The class works as a controller layer between the views and the backend
 * logic. There are no member variables so the functions are all
 * essentially static.
 * @author Fouad
 */
public class Controller {
	
	/**
	 * Checks if the internet connection is available on the device.
	 * @param connMgr Connectivity manager from the context
	 * @return true if internet available, else false
	 */
	public static boolean testInternetConnection (ConnectivityManager connMgr){
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        } else {
            return false;
        }
	}
	
	/**
	 * Calls OCR function by creating its own connection handler instance
	 * @param bMap The bitmap
	 * @param language Language of text
	 * @return Extracted text
	 * @throws IOException
	 */
	public static String runOCR(Bitmap bMap, String language) throws IOException{
		return runOCR(bMap, language, new ConnectionHandler());
	}
	
	/**
	 * Calls OCR function by using the connection handler instance provided by the View
	 * @param bMap The bitmap
	 * @param language Language of the text
	 * @param connection Connection handler instance
	 * @return Extracted text
	 * @throws IOException
	 */
	public static String runOCR(
			Bitmap bMap, 
			String language, 
			ConnectionHandler connection) 
					throws IOException{
		
		// Convert the bitmap to jpeg
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bMap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
		
		// Convert jpeg to base64 string
		byte[] imageBytes = baos.toByteArray();
		String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
		
		// Run OCR and return the string
		String result = connection.runRemoteOCR(
				encodedImage,
				language.toLowerCase().substring(0,3));
		
		return result;
	}
	
	/**
	 * Calls translator function by creating its own connection handler
	 * @param text Text to be translated
	 * @param fromLanguage Language of text
	 * @param toLanguage Language to which the text will be translated
	 * @return Translated text
	 * @throws IOException
	 */
	public static String runTranslator(
			String text, 
			String fromLanguage, 
			String toLanguage) 
					throws IOException{
		
		return runTranslator(text, fromLanguage, toLanguage, new ConnectionHandler());
	}
	
	/**
	 * Calls translator by using the connection handler instance provided by the view
	 * @param text Text to be translated
	 * @param fromLanguage Language of text
	 * @param toLanguage Language to which the text will be translated
	 * @param connection Connection handler instance
	 * @return Translated text
	 * @throws IOException
	 */
	public static String runTranslator(
			String text, 
			String fromLanguage, 
			String toLanguage,
			ConnectionHandler connection) 
					throws IOException{
		
		return connection.runRemoteTranslator(text, fromLanguage, toLanguage);
	}
	
	/**
	 * Asks the Locationhandler for the country of the device.
	 * @param geocoder
	 * @param locationManager
	 * @return
	 */
	public static String getLocation (
			Geocoder geocoder, 
			LocationManager locationManager){
		
		LocationHandler handler = new LocationHandler();
		String countryName = handler.getCountry(geocoder, locationManager);
		return countryName;
	}
}
