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
    public static JSONArray readJSONArray(Context context, int resource) throws JSONException {
        ArrayList<LatLng> list = new ArrayList<LatLng>();
        InputStream inputStream = context.getResources().openRawResource(resource);
        String json = new Scanner(inputStream).useDelimiter("\\A").next();
        return new JSONArray(json);
    }
    public static ArrayList<Crime> getCrimesFromJSONArray(JSONArray jsonArray) throws JSONException {
        ArrayList<Crime> crimes = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonCrime = jsonArray.getJSONObject(i);
            crimes.add(new Crime(jsonCrime));
        }
        return crimes;
    }

}
