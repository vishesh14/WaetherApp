package com.waetherapp.app.location;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;
import android.location.Location;
import java.util.List;
import java.util.Locale;

public class CityFinder {

    public static void setLongitudeLatitude(Location location) {
        try {
            LocationCall.lat = String.valueOf(location.getLatitude());
            LocationCall.lon = String.valueOf(location.getLongitude());
            Log.d("location_lat", LocationCall.lat);
            Log.d("location_lon", LocationCall.lon);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public static String getCityNameUsingNetwork(Context context, Location location) {
        String city = "";
        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            city = addresses.get(0).getLocality();
            Log.d("city", city);
        } catch (Exception e) {
            Log.d("city", "Error to find the city.");
        }
        return city;
    }
}