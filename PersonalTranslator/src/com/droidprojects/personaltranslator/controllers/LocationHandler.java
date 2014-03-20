package com.droidprojects.personaltranslator.controllers;

import java.util.List;

import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;

/**
 * Handles determination of the the device's location based on the GPS coordinates.
 * @author Fouad
 */
public class LocationHandler {

	/**
	 * Takes in utilities from the context and uses them to find the country of the user.
	 * @param geocoder Geocoder specific to the context
	 * @param locationManager Location manager specific to the context
	 * @return The country of the device
	 */
	public String getCountry(Geocoder geocoder, LocationManager locationManager){
		
		// Get location coordinates
		Location loc = getLocation(locationManager);
	    
		if (loc!=null){
			try {
				
				// Determine the country
		        List<Address> adr = geocoder.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
		        if (!adr.isEmpty()){
		        	return adr.get(0).getCountryName();
		        }
		    } catch (Exception e) {
		        e.printStackTrace();
		    }
		}
		
		// If country cannot be determined, return null
		return null;
	}
	
	/**
	 * Gets location using the location manager
	 * @param locationManager
	 * @return
	 */
	private Location getLocation(LocationManager locationManager){
        Criteria criteria = new Criteria();
        
        // Choose the provider that seems best. We want rough idea about the location.
        String provider = locationManager.getBestProvider(criteria, false);
        return locationManager.getLastKnownLocation(provider);
	}
}
