package example.com.soopa.server;

import android.content.Context;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.heatmaps.WeightedLatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import example.com.soopa.R;
import example.com.soopa.model.Crime;

public class Server {


    private Context context;
    private RequestQueue mRequestQueue;
    private boolean crimeLoading = false;

    public Server(Context context) {
        this.context = context;
        mRequestQueue = Volley.newRequestQueue(context);
    }

    /**
     * Fetches live crime data from the server and converts them into ArrayList of Crime objects
     * @param callback Callback to pass data
     * @param location The point for which we want the crime data (usually our location)
     * @param radius Radius in which we want to look for crimes (around location)
     * @param superhero Name of the superhero for which we want to search for crimes
     */
    public void getLiveCrimeData(final Utils.Callback<ArrayList<Crime>,String> callback, LatLng location, double radius, String superhero) {
        if(Utils.isReadyForQuery(context)) {
            String latQuery = "lat=" + location.latitude;
            String lngQuery = "lng=" + location.longitude;
            String radiusQuery = "radius=" + Double.toString(radius);
            String superQuery = "superhero=" + superhero;
            String url = Constants.API_URL + "active_crimes?" + latQuery + "&" + lngQuery + "&" + radiusQuery + "&" + superQuery;
            System.out.println(url);
            JsonArrayRequest jsonObjectRequest = new JsonArrayRequest(Request.Method.GET, url, null,
                    new Response.Listener<JSONArray>() {
                        @Override
                        public void onResponse(JSONArray jsonArray) {
                            crimeLoading = false;
                            try {
                                callback.onSuccess(Crime.fromJSONArray(jsonArray));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    }, new Utils.ServerResponse<ArrayList<Crime>>().simpleError(callback)) {
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }
            };
            jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(15000,
                    1,
                    1));
            mRequestQueue.add(jsonObjectRequest);
            crimeLoading = true;
        } else {
            callback.onFail(Constants.CONNECTION_ERROR);
        }
//        try {
//            JSONArray jsonArray = Utils.readJSONArray(context, R.raw.crime_data);
//            callback.onSuccess(Crime.fromJSONArray(jsonArray));
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }

    }
    public void getHeatMap(final Utils.Callback<ArrayList<WeightedLatLng>, String> callback) {
        if(Utils.isReadyForQuery(context)) {
            String url = Constants.API_URL + "heatmap";
            JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null,
                    new Response.Listener<JSONArray>() {
                        @Override
                        public void onResponse(JSONArray jsonArray) {
                            ArrayList<WeightedLatLng> points = new ArrayList<>();
                            try {
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONArray pointJSON = jsonArray.getJSONArray(i);
                                    WeightedLatLng point = new WeightedLatLng(new LatLng(pointJSON.getDouble(0),pointJSON.getDouble(1)),pointJSON.getDouble(2));
                                    points.add(point);
                                }
                            } catch(Exception e) {
                                e.printStackTrace();
                            }
                            callback.onSuccess(points);

                        }
                    }, new Utils.ServerResponse<ArrayList<WeightedLatLng>>().simpleError(callback)) {
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }
            };
            mRequestQueue.add(jsonArrayRequest);
        } else {
            callback.onFail(Constants.CONNECTION_ERROR);
        }

    }

    /**
     * Fetches directions from Mapquest API between two points and extracts all turning points into a list
     * @param callback Callback to pass data
     * @param from Starting point
     * @param to Ending point
     */
    public void getDirections(final Utils.Callback<ArrayList<LatLng>,String> callback, LatLng from, LatLng to) {
        if(Utils.isReadyForQuery(context)) {
            String fromString = "from="+Double.toString(from.latitude)+","+Double.toString(from.longitude);
            String toString = "to="+Double.toString(to.latitude)+","+Double.toString(to.longitude);
            String url = Constants.DIRECTIONS_API_URL + context.getString(R.string.mapquest_api_key) + "&" + fromString + "&" + toString;
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject jsonObject) {
                            ArrayList<LatLng> points = new ArrayList<>();
                            try {
                                JSONObject routeJSON = jsonObject.getJSONObject("route");
                                JSONArray legsJSONArray = routeJSON.getJSONArray("legs");
                                JSONObject legJSON = legsJSONArray.getJSONObject(0);
                                JSONArray maneuversJSONArray = legJSON.getJSONArray("maneuvers");
                                for (int i = 0; i < maneuversJSONArray.length(); i++) {
                                    JSONObject maneuver = maneuversJSONArray.getJSONObject(i);
                                    JSONObject startPointJSON = maneuver.getJSONObject("startPoint");
                                    LatLng point = new LatLng(startPointJSON.getDouble("lat"), startPointJSON.getDouble("lng"));
                                    points.add(point);
                                }
                            } catch(Exception e) {
                                e.printStackTrace();
                            }
                            callback.onSuccess(points);
                        }
                    }, new Utils.ServerResponse<ArrayList<LatLng>>().simpleError(callback)) {
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }
            };
            mRequestQueue.add(jsonObjectRequest);
        } else {
            callback.onFail(Constants.CONNECTION_ERROR);
        }
    }

    /**
     * Sends request to Mapquest API for a route between two points and extracts sessionId which will be later used in
     * getRouteShape() to get the shape of the route
     * This procedure gives more accurate shape than getDirections()
     * @param callback Callback to pass data
     * @param from Starting point
     * @param to Ending point
     */
    public void getSessionId(final Utils.Callback<String,String> callback, LatLng from, LatLng to) {
        if(Utils.isReadyForQuery(context)) {
            String fromString = "from="+Double.toString(from.latitude)+","+Double.toString(from.longitude);
            String toString = "to="+Double.toString(to.latitude)+","+Double.toString(to.longitude);
            String url = Constants.DIRECTIONS_API_URL + context.getString(R.string.mapquest_api_key) + "&" + fromString + "&" + toString;
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject jsonObject) {
                            ArrayList<LatLng> points = new ArrayList<>();
                            String session = "";
                            try {
                                JSONObject routeJSON = jsonObject.getJSONObject("route");
                                session = routeJSON.getString("sessionId");

                            } catch(Exception e) {
                                e.printStackTrace();
                            }
                            callback.onSuccess(session);
                        }
                    }, new Utils.ServerResponse<String>().simpleError(callback)) {
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }
            };
            mRequestQueue.add(jsonObjectRequest);
        } else {
            callback.onFail(Constants.CONNECTION_ERROR);
        }
    }

    /**
     * Sends request to Mapquest API for a route shape based on a sessionId fetched earlier in getSessionId()
     * More accurate than getDirections()
     * @param callback Callback to pass data
     * @param from Starting point. Used only to center the map. Required argument by the API
     * @param sessionId SessionId fetched in getSessionId()
     */
    public void getRouteShape(final Utils.Callback<ArrayList<LatLng>,String> callback, LatLng from, String sessionId) {
        if(Utils.isReadyForQuery(context)) {
            String url = Constants.ROUTE_SHAPE_API_URL + context.getString(R.string.mapquest_api_key) + "&sessionId="+sessionId+"&mapWidth=320&mapHeight=240&mapScale=1733371&mapLat="+Double.toString(from.latitude)+"&mapLng="+Double.toString(from.longitude);
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject jsonObject) {
                            ArrayList<LatLng> points = new ArrayList<>();
                            try {
                                JSONObject routeJSON = jsonObject.getJSONObject("route");
                                JSONObject shapeJSON = routeJSON.getJSONObject("shape");
                                JSONArray shapePointsJSONArray = shapeJSON.getJSONArray("shapePoints");
                                for (int i = 0; i < shapePointsJSONArray.length(); i=i+2) {
                                    double lat = shapePointsJSONArray.getDouble(i);
                                    double lng = shapePointsJSONArray.getDouble(i+1);
                                    LatLng point = new LatLng(lat,lng);
                                    points.add(point);
                                }
                            } catch(Exception e) {
                                e.printStackTrace();
                            }
                            callback.onSuccess(points);
                        }
                    }, new Utils.ServerResponse<ArrayList<LatLng>>().simpleError(callback)) {
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }
            };
            mRequestQueue.add(jsonObjectRequest);
        } else {
            callback.onFail(Constants.CONNECTION_ERROR);
        }
    }
}
