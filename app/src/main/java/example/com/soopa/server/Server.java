package example.com.soopa.server;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

import example.com.soopa.R;
import example.com.soopa.Utils;
import example.com.soopa.model.Crime;

public class Server {


    private Context context;
    private RequestQueue mRequestQueue;

    public Server(Context context) {
        this.context = context;
        mRequestQueue = Volley.newRequestQueue(context);
    }

    public void getLiveCrimeData(final ServerUtils.Callback<ArrayList<Crime>,String> callback) {
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
        try {
            JSONArray jsonArray = Utils.readJSONArray(context, R.raw.crime_data);
            callback.onSuccess(Crime.fromJSONArray(jsonArray));
        } catch (JSONException e) {
            e.printStackTrace();
        }



    }
    public void getHeatMap(final ServerUtils.Callback<String,String> callback) {

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


}
