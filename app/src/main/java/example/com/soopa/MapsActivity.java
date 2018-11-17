package example.com.soopa;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;

import example.com.soopa.server.Server;
import example.com.soopa.server.ServerUtils;
import example.com.soopa.model.Crime;
import example.com.soopa.model.VantagePoint;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private HeatmapTileProvider mProvider;
    private Server mServer;
    private TileOverlay mOverlay;
    private Marker currentMarker;
    private ArrayList<Marker> previousCrimeMarkers = new ArrayList<>();
    private ArrayList<Marker> previousVantageMarkers = new ArrayList<>();
    private HashMap<String, VantagePoint> previousVantagePoints = new HashMap<>();
    private HashMap<String, Crime> previousCrimes = new HashMap<>();
    private boolean done = false;
    private boolean locationSet = false;
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private Marker currentLocation;
    public static final int LOCATION_REFRESH_PERIOD = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mServer = new Server(this);
        // Acquire a reference to the system Location Manager
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        mLocationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                updateCurrentLocation(new LatLng(location.getLatitude(), location.getLongitude()));
                if (!locationSet) {
                    if (done) {
                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                        for (Marker marker2 : previousCrimeMarkers) {
                            builder.include(marker2.getPosition());
                        }
                        builder.include(new LatLng(location.getLatitude(), location.getLongitude()));
                        LatLngBounds bounds = builder.build();
                        int width = getResources().getDisplayMetrics().widthPixels;
                        int height = getResources().getDisplayMetrics().heightPixels;
                        int padding = (int) (width * 0.2); // offset from edges of the map 20% of screen
                        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding));
                        locationSet = true;
                        System.out.println("OKOKOKO");
                    }
                }
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };

        checkLocationPermission();
    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    public boolean checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.ACCESS_FINE_LOCATION},MY_PERMISSIONS_REQUEST_LOCATION);
            return false;
        } else {
            return true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_PERIOD, 0, mLocationListener);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationManager.removeUpdates(mLocationListener);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_PERIOD, 0, mLocationListener);
                    }
                } else {

                }
            }
        }
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if(previousCrimes.containsKey(marker.getId())) {
                    cleanPreviousVantagePoints();
                    addVantagePoints(previousCrimes.get(marker.getId()).getVantagePoints());
                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    for (Marker vP : previousVantageMarkers) {
                        builder.include(vP.getPosition());
                    }
                    builder.include(marker.getPosition());
                    LatLngBounds bounds = builder.build();
                    int width = getResources().getDisplayMetrics().widthPixels;
                    int height = getResources().getDisplayMetrics().heightPixels;
                    int padding = (int) (width * 0.2); // offset from edges of the map 20% of screen
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding));
                }
                marker.showInfoWindow();
                return true;
            }
        });
        mServer.getLiveCrimeData(new ServerUtils.Callback<ArrayList<Crime>, String>() {
            @Override
            public void onSuccess(ArrayList<Crime> crimeList) {
                cleanPreviousCrimes();
                addCrimes(crimeList);
            }

            @Override
            public void onFail(String obj) {

            }
        });
        //addHeatMap();

    }

    private void cleanPreviousCrimes() {
        for(Marker marker : previousCrimeMarkers) {
            marker.remove();
        }
        previousCrimeMarkers.clear();
        previousCrimes.clear();
    }

    private void cleanPreviousVantagePoints() {
        for(Marker vantagePoint : previousVantageMarkers) {
            vantagePoint.remove();
        }
        previousVantageMarkers.clear();
        previousVantagePoints.clear();
    }

    private void addCrimes(ArrayList<Crime> crimeList) {
        int iconHeight = 100;
        int iconWidth = 100;
        BitmapDrawable originalIcon = (BitmapDrawable)getResources().getDrawable(R.drawable.crime);
        Bitmap b = originalIcon.getBitmap();
        Bitmap smallIcon = Bitmap.createScaledBitmap(b, iconWidth, iconHeight, false);
        for(Crime crime : crimeList) {
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(crime.getLocation());
            markerOptions.title(crime.getCrimeType());
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(smallIcon));
            Marker marker = mMap.addMarker(markerOptions);
            previousCrimeMarkers.add(marker);
            previousCrimes.put(marker.getId(),crime);
        }
        if(!done) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (Marker marker2 : previousCrimeMarkers) {
                builder.include(marker2.getPosition());
            }
            LatLngBounds bounds = builder.build();
            int width = getResources().getDisplayMetrics().widthPixels;
            int height = getResources().getDisplayMetrics().heightPixels;
            int padding = (int) (width * 0.2); // offset from edges of the map 20% of screen
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding));
            done = true;
        }


    }

    private void addVantagePoints(ArrayList<VantagePoint> vantagePoints) {
        for(VantagePoint vantagePoint : vantagePoints) {
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(vantagePoint.getPosition());
            markerOptions.title(Double.toString(vantagePoint.getHeight()));
            Marker marker = mMap.addMarker(markerOptions);
            previousVantageMarkers.add(marker);
            previousVantagePoints.put(marker.getId(),vantagePoint);
        }
    }

    private void updateCurrentLocation(LatLng position) {
        int iconHeight = 100;
        int iconWidth = 100;
        BitmapDrawable originalIcon = (BitmapDrawable)getResources().getDrawable(R.drawable.current_location);
        Bitmap b = originalIcon.getBitmap();
        Bitmap smallIcon = Bitmap.createScaledBitmap(b, iconWidth, iconHeight, false);
        if(currentLocation != null) {
            Marker marker = currentLocation;
            MarkerAnimation.animateMarkerToICS(marker, position, new LatLngInterpolator.Spherical());
        } else {
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(position);
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(smallIcon));
            currentLocation = mMap.addMarker(markerOptions);
        }
    }

    private void addHeatMap() {
        ArrayList<LatLng> list = new ArrayList<>();
        try {
            list = Utils.readPoints(this,R.raw.json_data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mProvider = new HeatmapTileProvider.Builder()
                .data(list)
                .build();
        // Add a tile overlay to the map, using the heat map tile provider.
        mOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
    }


}
