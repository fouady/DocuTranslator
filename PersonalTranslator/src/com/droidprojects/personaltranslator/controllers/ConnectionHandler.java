package com.droidprojects.personaltranslator.controllers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.droidprojects.personaltranslator.constants.Constants;

import android.util.Base64;

/**
 * This class works as an abstraction between the Android client and server(s).
 * It handles the http connection, query creation and parsing of the results.
 * @author Fouad
 */
public class ConnectionHandler {
	
	private HttpURLConnection 	mConnection;				// Connection instance
	private boolean 			mTerminateConnection=false;	// Flag to terminate connection
	
	/**
	 * Sets up data to be sent to server for translation and parses the received data
	 * @param text Text on which the OCR should be run
	 * @param fromLanguage
	 * @param toLanguage
	 * @return The translated text
	 * @throws IOException
	 */
	public String runRemoteTranslator(
			String text, 
			String fromLanguage, 
			String toLanguage) 
					throws IOException {
		
		// Bing translator recognizes carriage return 
		text = text.replace("\n", "\r\n");
		
		// Set up name value pairs
		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("From", "'"+fromLanguage+"'"));
		params.add(new BasicNameValuePair("To", "'"+toLanguage+"'"));
		params.add(new BasicNameValuePair("Text", "'"+text+"'"));
		
		// Set the data as part of URL
		String url = Constants.SERVER_URL_TRANSLATOR_BING+"?"+getQuery(params);

		// Set authorization credentials
		String auth = Base64.encodeToString(("ignored:"+Constants.SERVER_PRIMARY_KEY_BING)
				.getBytes(), Base64.DEFAULT);
		ArrayList<NameValuePair> requestProperties = new ArrayList<NameValuePair>();
		requestProperties.add(new BasicNameValuePair("Authorization", "Basic "+auth));
		
		String result = sendReceive(url, null, requestProperties);

		// Extract the required data from the returned string
		String translation=null;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new StringReader(result)));
			translation = doc.getElementsByTagName("d:Text").item(0).getTextContent();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		}
        
        return translation;
	}
	
	/**
	 * 
	 * @param encodedImage Base64 encoded image for OCR extraction
	 * @param language Language of the text in image
	 * @return The extracted text string
	 * @throws IOException
	 */
	public String runRemoteOCR(String encodedImage, String language) throws IOException{
		
		// Set up data to be sent
		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("image", encodedImage));
		params.add(new BasicNameValuePair("language", language));
		
		return sendReceive(Constants.SERVER_URL_OCR, params, null);
	}
	
	/**
	 * Establishes connection, sets up parameters and receives data
	 * @param serverURL
	 * @param params The get/post parameters
	 * @param requestProperties
	 * @return Response from server
	 * @throws IOException
	 */
	public String sendReceive(
			String serverURL, 
			List<NameValuePair>	params,
			List<NameValuePair>	requestProperties) 
					throws IOException {
	    
		InputStream is = null;
	        
	    try {
	        URL url = new URL(serverURL);
	        mConnection = (HttpURLConnection) url.openConnection();
	        mConnection.setReadTimeout(50000 /* milliseconds */); 
	        mConnection.setConnectTimeout(40000 /* milliseconds */);
	        mConnection.setRequestMethod("POST");
	        mConnection.setDoInput(true);
	        mConnection.setDoOutput(true);

	        // Set authorization and other properties, if they exist
	        if (requestProperties!=null)
		        for (NameValuePair nvp : requestProperties){
		        	mConnection.setRequestProperty(nvp.getName(), nvp.getValue());
		        }
	        
	        // Set post parameters, if they exist
	        if (params!=null){
		        OutputStream os = mConnection.getOutputStream();
		        BufferedWriter writer = new BufferedWriter(
		                new OutputStreamWriter(os, "UTF-8"));
		        writer.write(getQuery(params));
		        writer.flush();
		        writer.close();
		        os.close();
	        }
	        
	        // Just before establishing connection, see if there is a request from the UI to
	        // terminate connection. If so, return null.
	        if(mTerminateConnection)
	        	return null;
	        
	        // Starts the query
	        mConnection.connect();
	        int response = mConnection.getResponseCode();
	        is = mConnection.getInputStream();
	        
	        // Convert the InputStream into a string
	        String contentAsString = convertStreamToString(is);
	        
	        return contentAsString;
	        
	    // Makes sure that the InputStream is closed after the app is
	    // finished using it.
	    } finally {
	        if (is != null) {
	            is.close();
	        } 
	    }
	}
	
	/**
	 * Reads and inputstream and converts it to string
	 * @param stream
	 * @return
	 * @throws IOException
	 * @throws UnsupportedEncodingException
	 */
	private String convertStreamToString(InputStream stream) 
			throws IOException, UnsupportedEncodingException {
		
	    BufferedReader reader = new BufferedReader(
	    		new InputStreamReader(stream,"UTF-8"));
	    
	    StringBuilder out = new StringBuilder();
	    String buffer;
	    while ((buffer=reader.readLine()) != null){
	    	out.append(buffer+"\n");
	    }
	    reader.close();
	    return out.toString();
	}
	
	/**
	 * Changes the name-value pairs to a get/post query
	 * @param params The nvps
	 * @return The query string
	 * @throws UnsupportedEncodingException
	 */
	private String getQuery(List<NameValuePair> params) throws UnsupportedEncodingException
	{
	    StringBuilder result = new StringBuilder();
	    boolean first = true;

	    for (NameValuePair pair : params)
	    {
	        if (first)
	            first = false;
	        else
	            result.append("&");

	        result.append(URLEncoder.encode(pair.getName(), "UTF-8"));
	        result.append("=");
	        result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
	    }
	    
	    return result.toString();
	}
	
	/**
	 * This function is called by the UI/Views in order to request the connection to terminate
	 */
	public void terminateConnection(){
		mTerminateConnection=true;
		if (mConnection!=null){
			mConnection.disconnect();
		}
	}
}
