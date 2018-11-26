package example.com.soopa;

import android.content.Context;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Scanner;

import example.com.soopa.model.Crime;
import example.com.soopa.model.VantagePoint;

public class Utils {
    /**
     * Reads points from JSON raw resource (primarily for offline testing)
     * @param context Context
     * @param resource Id of the resource
     * @return List of points (LatLng)
     * @throws JSONException when can't parse JSON file
     */
    public static ArrayList<LatLng> readPoints(Context context, int resource) throws JSONException {
        ArrayList<LatLng> list = new ArrayList<LatLng>();
        InputStream inputStream = context.getResources().openRawResource(resource);
        String json = new Scanner(inputStream).useDelimiter("\\A").next();
        JSONArray array = new JSONArray(json);
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.getJSONObject(i);
            double lat = object.getDouble("lat");
            double lng = object.getDouble("lng");
            list.add(new LatLng(lat, lng));
        }
        System.out.println(list.size());
        return list;
    }

    /**
     * Reads JSONArray from raw resource
     * @param context Context
     * @param resource Id of the resource
     * @return JSONArray of objects in JSON file
     * @throws JSONException when can't parse JSON file
     */
    public static JSONArray readJSONArray(Context context, int resource) throws JSONException {
        ArrayList<LatLng> list = new ArrayList<LatLng>();
        InputStream inputStream = context.getResources().openRawResource(resource);
        String json = new Scanner(inputStream).useDelimiter("\\A").next();
        return new JSONArray(json);
    }

    /**
     * Converts JSONArray (fetched from the server or loaded from raw file) to the list of Crime objects
     * @param jsonArray
     * @return list of crimes
     * @throws JSONException when can't parse JSON file
     */
    public static ArrayList<Crime> getCrimesFromJSONArray(JSONArray jsonArray) throws JSONException {
        ArrayList<Crime> crimes = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonCrime = jsonArray.getJSONObject(i);
            crimes.add(new Crime(jsonCrime));
        }
        return crimes;
    }

}
