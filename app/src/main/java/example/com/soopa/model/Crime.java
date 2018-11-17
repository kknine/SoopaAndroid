package example.com.soopa.model;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Crime {
    private final LatLng location;
    private final String crimeType;
    private final int crimeDuration;
    private final ArrayList<VantagePoint> vantagePoints;
    public static final String SERVER_LAT = "lat";
    public static final String SERVER_LNG = "lng";
    public static final String SERVER_CRIME_TYPE = "crime_type";
    public static final String SERVER_CRIME_DURATION = "crime_duration";
    public static final String SERVER_VANTAGE_POINTS = "vantage_points";

    public Crime(LatLng location, String crimeType, int crimeDuration, ArrayList<VantagePoint> vantagePoints) {
        this.location = location;
        this.crimeType = crimeType;
        this.crimeDuration = crimeDuration;
        this.vantagePoints = vantagePoints;
    }

    public Crime(JSONObject jsonCrime) throws JSONException {
        JSONArray jsonVantagePoints = jsonCrime.getJSONArray(SERVER_VANTAGE_POINTS);
        ArrayList<VantagePoint> vantagePoints = new ArrayList<>();
        for (int j = 0; j < jsonVantagePoints.length(); j++) {
            JSONObject jsonVP = jsonVantagePoints.getJSONObject(j);
            vantagePoints.add(new VantagePoint(new LatLng(jsonVP.getDouble(VantagePoint.SERVER_LAT), jsonVP.getDouble(VantagePoint.SERVER_LNG)), jsonVP.getDouble(VantagePoint.SERVER_HEIGHT)));
        }
        this.location = new LatLng(jsonCrime.getDouble(SERVER_LAT), jsonCrime.getDouble(Crime.SERVER_LNG));
        this.crimeType = jsonCrime.getString(Crime.SERVER_CRIME_TYPE);
        this.crimeDuration = jsonCrime.getInt(Crime.SERVER_CRIME_DURATION);
        this.vantagePoints = vantagePoints;
    }

    public LatLng getLocation() {
        return location;
    }

    public String getCrimeType() {
        return crimeType;
    }

    public int getCrimeDuration() {
        return crimeDuration;
    }

    public ArrayList<VantagePoint> getVantagePoints() {
        return vantagePoints;
    }

    public static ArrayList<Crime> fromJSONArray(JSONArray jsonArray) throws JSONException {
        ArrayList<Crime> crimes = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonCrime = jsonArray.getJSONObject(i);
            crimes.add(new Crime(jsonCrime));
        }
        return crimes;
    }
}
