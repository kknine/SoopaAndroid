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
import example.com.soopa.Utils;
import example.com.soopa.model.Crime;

public class Server {


    private Context context;
    private RequestQueue mRequestQueue;
    private boolean crimeLoading = false;

    public Server(Context context) {
        this.context = context;
        mRequestQueue = Volley.newRequestQueue(context);
    }

    public void getLiveCrimeData(final ServerUtils.Callback<ArrayList<Crime>,String> callback, LatLng location, double radius, String superhero) {
        if(ServerUtils.isReadyForQuery(context)) {
            String latQuery = "lat=" + location.latitude;
            String lngQuery = "lng=" + location.longitude;
            String radiusQuery = "radius=" + Double.toString(radius);
            String superQuery = "superhero=" + superhero;
            String url = ServerConstants.API_URL + "active_crimes?" + latQuery + "&" + lngQuery + "&" + radiusQuery + "&" + superQuery;
            System.out.println(url);
            JsonArrayRequest jsonObjectRequest = new JsonArrayRequest(Request.Method.GET, url, null,
                    new Response.Listener<JSONArray>() {
                        @Override
                        public void onResponse(JSONArray jsonArray) {
                            crimeLoading = false;
                            try {
                                System.out.println(jsonArray.length());
                                callback.onSuccess(Crime.fromJSONArray(jsonArray));

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    }, new ServerUtils.ServerResponse<ArrayList<Crime>>().simpleError(callback)) {
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
            callback.onFail(ServerConstants.CONNECTION_ERROR);
        }
//        try {
//            JSONArray jsonArray = Utils.readJSONArray(context, R.raw.crime_data);
//            callback.onSuccess(Crime.fromJSONArray(jsonArray));
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }

    }
    public void getHeatMap(final ServerUtils.Callback<ArrayList<WeightedLatLng>, String> callback) {
        if(ServerUtils.isReadyForQuery(context)) {
            String url = ServerConstants.API_URL + "heatmap";
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
                    }, new ServerUtils.ServerResponse<ArrayList<WeightedLatLng>>().simpleError(callback)) {
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }
            };
            mRequestQueue.add(jsonArrayRequest);
        } else {
            callback.onFail(ServerConstants.CONNECTION_ERROR);
        }

    }
    public void getHighestBuilding(final ServerUtils.Callback<LatLng, String> callback, LatLng point) {
//        if(ServerUtils.isReadyForQuery(context)) {
//            String url = ServerConstants.API_URL;
//            JsonArrayRequest jsonObjectRequest = new JsonArrayRequest(Request.Method.GET, url, null,
//                    new Response.Listener<JSONArray>() {
//                        @Override
//                        public void onResponse(JSONArray jsonArray) {
//                            callback.onSuccess(null);
//                        }
//                    }, new ServerUtils.ServerResponse<String>().simpleError(callback)) {
//                @Override
//                public String getBodyContentType() {
//                    return "application/json; charset=utf-8";
//                }
//            };
//            mRequestQueue.add(jsonObjectRequest);
//        } else {
//            callback.onFail(ServerConstants.CONNECTION_ERROR);
//        }
        LatLng building = new LatLng(51.6,0);
        callback.onSuccess(building);
    }

    public void getDirections(final ServerUtils.Callback<ArrayList<LatLng>,String> callback, LatLng from, LatLng to) {
        if(ServerUtils.isReadyForQuery(context)) {
            String fromString = "from="+Double.toString(from.latitude)+","+Double.toString(from.longitude);
            String toString = "to="+Double.toString(to.latitude)+","+Double.toString(to.longitude);
            String url = ServerConstants.DIRECTIONS_API_URL + "&" + fromString + "&" + toString;
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
                    }, new ServerUtils.ServerResponse<ArrayList<LatLng>>().simpleError(callback)) {
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }
            };
            mRequestQueue.add(jsonObjectRequest);
        } else {
            callback.onFail(ServerConstants.CONNECTION_ERROR);
        }
    }
    public void getSessionId(final ServerUtils.Callback<String,String> callback, LatLng from, LatLng to) {
        if(ServerUtils.isReadyForQuery(context)) {
            String fromString = "from="+Double.toString(from.latitude)+","+Double.toString(from.longitude);
            String toString = "to="+Double.toString(to.latitude)+","+Double.toString(to.longitude);
            String url = ServerConstants.DIRECTIONS_API_URL + "&" + fromString + "&" + toString;
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
                    }, new ServerUtils.ServerResponse<String>().simpleError(callback)) {
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }
            };
            mRequestQueue.add(jsonObjectRequest);
        } else {
            callback.onFail(ServerConstants.CONNECTION_ERROR);
        }
    }
    public void getRouteShape(final ServerUtils.Callback<ArrayList<LatLng>,String> callback, LatLng from, String sessionId) {
        if(ServerUtils.isReadyForQuery(context)) {
            String url = ServerConstants.ROUTE_SHAPE_API_URL + "&sessionId="+sessionId+"&mapWidth=320&mapHeight=240&mapScale=1733371&mapLat="+Double.toString(from.latitude)+"&mapLng="+Double.toString(from.longitude);
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
                    }, new ServerUtils.ServerResponse<ArrayList<LatLng>>().simpleError(callback)) {
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }
            };
            mRequestQueue.add(jsonObjectRequest);
        } else {
            callback.onFail(ServerConstants.CONNECTION_ERROR);
        }
    }


}
