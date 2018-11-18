package example.com.soopa;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;

import org.json.JSONException;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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
    private ArrayList<Marker> displayedCrimeMarkers = new ArrayList<>();
    private ArrayList<Marker> displayedVantageMarkers = new ArrayList<>();
    private HashMap<String, VantagePoint> displayedVantagePoints = new HashMap<>();
    private HashMap<String, Crime> displayedCrimes = new HashMap<>();
    private boolean done = false;
    private boolean locationSet = false;
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private Marker currentLocation = null;
    public static final int LOCATION_REFRESH_PERIOD = 2000;
    public static final String[] SUPERHEROES = {"Iron Man", "Spider-Man", "Hulk"};
    private Polyline route;
    private int currentSuperNum = 0;
    private String chosenVantagePointId = null;
    private boolean locationDetermined = false;
    private boolean started = false;
    ScheduledExecutorService mScheduler;
    TimerTask mTask;
    ScheduledFuture<?> mScheduledFuture;
    Timer mTimer;
    private boolean crimeLoading = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                updateSuperHero();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mServer = new Server(this);

        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        mLocationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                System.out.println("location");
                // Called when a new location is found by the network location provider.
                updateCurrentLocation(new LatLng(location.getLatitude(), location.getLongitude()));

                if (!locationSet) {
                    if (done) {
                        ArrayList<LatLng> points = new ArrayList<>();
                        for(Marker crime : displayedCrimeMarkers) {
                            points.add(crime.getPosition());
                        }
                        points.add(currentLocation.getPosition());
                        centerCamera(points);
                        locationSet = true;
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
        if(started) {
            if(mTimer==null) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        startUpdatingHeatMap();
                    }
                }).start();
            }
        }
    }

    private void startUpdatingHeatMap(){
        System.out.println("fired");
        started = true;
        final LatLng position = currentLocation.getPosition();
        mTimer = new Timer();
        mTask = new TimerTask() {
            @Override
            public void run() {
                mServer.getHeatMap(new ServerUtils.Callback<ArrayList<WeightedLatLng>, String>() {
                    @Override
                    public void onSuccess(ArrayList<WeightedLatLng> list) {
                        addHeatMap(list);
                    }

                    @Override
                    public void onFail(String obj) {
                        System.out.println(obj);
                    }
                });
                if(!crimeLoading) {
                    mServer.getLiveCrimeData(new ServerUtils.Callback<ArrayList<Crime>, String>() {
                        @Override
                        public void onSuccess(final ArrayList<Crime> crimeList) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    System.out.println(crimeList.size());
                                    cleanPreviousCrimes();
                                    addCrimes(crimeList);
                                    crimeLoading = false;
                                }
                            });

                        }

                        @Override
                        public void onFail(String obj) {
                            System.out.println(obj);
                            crimeLoading = false;
                        }
                    }, position, 30, "Spiderman");
                    crimeLoading = true;
                }
            }

        };
        mTimer.schedule(mTask, 5000, 5000);

    }

    protected void onDestroy() {
        super.onDestroy();
    }
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void onPause() {
        super.onPause();
        System.out.println("onPause");
        if(mTimer!=null) {
            mTimer.cancel();
            mTimer.purge();
        }
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


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(final Marker marker) {
                if(displayedCrimes.containsKey(marker.getId())) {
                    final Crime crime = displayedCrimes.get(marker.getId());
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MapsActivity.this);
                    LayoutInflater inflater = MapsActivity.this.getLayoutInflater();

                    View dialogView= inflater.inflate(R.layout.dialog, null);
                    dialogBuilder.setView(dialogView);
                    ImageView iv = (ImageView) dialogView.findViewById(R.id.imageView);
                    iv.setImageDrawable(getResources().getDrawable(getResourceForCrime(crime.getCrimeType())));
                    TextView type = (TextView) dialogView.findViewById(R.id.crime_type);
                    type.setText(crime.getCrimeType());
                    TextView duration = (TextView) dialogView.findViewById(R.id.estimated);
                    duration.setText("Estimated time: " + Integer.toString((int)crime.getCrimeDuration()));
                    TextView severity = (TextView) dialogView.findViewById(R.id.severity);
                    severity.setText("Severity: " + Integer.toString(crime.getSeverity()));

                    Button button = (Button)dialogView.findViewById(R.id.accept);
                    final AlertDialog dialog = dialogBuilder.create();
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            cleanPreviousVantagePoints();
                            addVantagePoints(crime.getVantagePoints());
                            LatLngBounds.Builder builder = new LatLngBounds.Builder();
                            for (Marker vP : displayedVantageMarkers) {
                                builder.include(vP.getPosition());
                            }
                            builder.include(marker.getPosition());
                            LatLngBounds bounds = builder.build();
                            int width = getResources().getDisplayMetrics().widthPixels;
                            int height = getResources().getDisplayMetrics().heightPixels;
                            int padding = (int) (width * 0.2); // offset from edges of the map 20% of screen
                            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding));
                            dialog.cancel();
                        }
                    });
                    dialog.show();

                } else if(displayedVantageMarkers.contains(marker)) {
                    final VantagePoint vP = displayedVantagePoints.get(marker.getId());
                    AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                    builder.setTitle("Tall building");
                    builder.setMessage("Height: " + Double.toString(vP.getHeight()) + " meters");
                    builder.setPositiveButton("Navigate", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            navigate(marker.getId());
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    });
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();


                }
                marker.showInfoWindow();
                return true;
            }
        });




    }

    private void cleanPreviousCrimes() {
        for(Marker marker : displayedCrimeMarkers) {
            marker.remove();
        }
        displayedCrimeMarkers.clear();
        displayedCrimes.clear();
    }

    private void cleanPreviousVantagePoints() {
        for(Marker vantagePoint : displayedVantageMarkers) {
            vantagePoint.remove();
        }
        displayedVantageMarkers.clear();
        displayedVantagePoints.clear();
    }

    private void addCrimes(ArrayList<Crime> crimeList) {
        int prevNumOfCrimes = displayedCrimeMarkers.size();
        int iconHeight = 100;
        int iconWidth = 100;
        for(Crime crime : crimeList) {
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(crime.getLocation());
            markerOptions.title(crime.getCrimeType());
            BitmapDrawable originalIcon = (BitmapDrawable)getResources().getDrawable(getResourceForCrime(crime.getCrimeType()));
            Bitmap b = originalIcon.getBitmap();
            Bitmap smallIcon = Bitmap.createScaledBitmap(b, iconWidth, iconHeight, false);
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(smallIcon));
            Marker marker = mMap.addMarker(markerOptions);
            displayedCrimeMarkers.add(marker);
            displayedCrimes.put(marker.getId(),crime);
        }
        if((!done)||(prevNumOfCrimes<displayedCrimeMarkers.size())) {
            ArrayList<LatLng> points = new ArrayList<>();
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (Marker marker2 : displayedCrimeMarkers) {
                points.add(marker2.getPosition());
            }
            if(!points.isEmpty()) {
                points.add(currentLocation.getPosition());
                centerCamera(points);
                done = true;
            }

        }


    }

    private void addVantagePoints(ArrayList<VantagePoint> vantagePoints) {
        int iconHeight = 100;
        int iconWidth = 100;
        BitmapDrawable originalIcon = (BitmapDrawable)getResources().getDrawable(R.drawable.building);
        Bitmap b = originalIcon.getBitmap();
        Bitmap smallIcon = Bitmap.createScaledBitmap(b, iconWidth, iconHeight, false);
        for(VantagePoint vantagePoint : vantagePoints) {
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(vantagePoint.getPosition());
            markerOptions.title(Double.toString(vantagePoint.getHeight()));
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(smallIcon));
            Marker marker = mMap.addMarker(markerOptions);
            displayedVantageMarkers.add(marker);
            displayedVantagePoints.put(marker.getId(),vantagePoint);
        }
    }

    private void updateCurrentLocation(LatLng position) {
        if(currentLocation != null) {
            if((Math.abs(position.longitude-currentLocation.getPosition().longitude)>0.001)||(Math.abs(position.latitude-currentLocation.getPosition().latitude)>0.001)) {
                Marker marker = currentLocation;
                MarkerAnimation.animateMarkerToICS(marker, position, new LatLngInterpolator.Spherical());
                if (mTimer != null) {
                    mTimer.cancel();
                    mTimer.purge();
                    mTimer = null;
                }

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startUpdatingHeatMap();
                    }
                }, 3000);

            }
        } else {
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(position);
            int iconHeight = 128;
            int iconWidth = 128;
            int superNum = getSuperHeroNum();
            int resource = R.drawable.current_location;
            switch(superNum) {
                case 0:
                    resource = R.drawable.iron_man;
                    break;
                case 1:
                    resource = R.drawable.spider_man;
                    break;
                case 2:
                    resource = R.drawable.hulk;
                    break;
            }
            BitmapDrawable originalIcon = (BitmapDrawable)getResources().getDrawable(resource);
            Bitmap b = originalIcon.getBitmap();
            Bitmap smallIcon = Bitmap.createScaledBitmap(b, iconWidth, iconHeight, false);
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(smallIcon));
            currentLocation = mMap.addMarker(markerOptions);
            startUpdatingHeatMap();
        }


    }

    public void showMeAndCrimes(View v) {
        ArrayList<LatLng> points = new ArrayList<>();
        for(Marker crime : displayedCrimeMarkers) {
            points.add(crime.getPosition());
        }
        points.add(currentLocation.getPosition());
        centerCamera(points);
    }

    private void navigate(String vPId) {
        int superNum = getSuperHeroNum();
        chosenVantagePointId = vPId;
        final LatLng target = displayedVantagePoints.get(vPId).getPosition();
        switch (superNum) {
            case 0: // Iron Man
                if (route != null)
                    route.remove();
                ArrayList<LatLng> points = new ArrayList<>();
                points.add(currentLocation.getPosition());
                points.add(target);
                centerCamera(points);
                route = addPolyline(points);
                break;
            case 1: // Spider-Man
//                mServer.getDirections(new ServerUtils.Callback<ArrayList<LatLng>, String>() {
//                    @Override
//                    public void onSuccess(ArrayList<LatLng> points) {
//                        if (route != null)
//                            route.remove();
//                        route = addPolyline(points);
//                        centerCamera(points);
//                    }
//
//                    @Override
//                    public void onFail(String obj) {
//
//                    }
//                }, currentLocation.getPosition(), target);
                mServer.getSessionId(new ServerUtils.Callback<String, String>() {
                    @Override
                    public void onSuccess(final String sessionId) {
                        mServer.getRouteShape(new ServerUtils.Callback<ArrayList<LatLng>, String>() {
                            @Override
                            public void onSuccess(ArrayList<LatLng> points) {
                                if (route != null)
                                    route.remove();
                                route = addPolyline(points);
                                centerCamera(points);
                            }

                            @Override
                            public void onFail(String obj) {

                            }
                        },target,sessionId);
                    }

                    @Override
                    public void onFail(String obj) {

                    }
                },currentLocation.getPosition(), target);
                break;
            case 2: // Hulk
                break;
        }


    }

    private Polyline addLineSegment(LatLng point1, LatLng point2) {
        PolylineOptions polylineOptions = new PolylineOptions();
        polylineOptions.add(point1).add(point2);
        return mMap.addPolyline(polylineOptions);
    }

    private Polyline addPolyline(ArrayList<LatLng> points) {
        PolylineOptions polylineOptions = new PolylineOptions();
        for(LatLng point : points) {
            polylineOptions.add(point);
        }
        return mMap.addPolyline(polylineOptions);
    }


    private void centerCamera(ArrayList<LatLng> points) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng point : points) {
            builder.include(point);
        }
        LatLngBounds bounds = builder.build();
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        int padding = (int) (width * 0.2); // offset from edges of the map 20% of screen
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding),1000,null);
    }

    private int getSuperHeroNum() {
        Spinner spinner = (Spinner)findViewById(R.id.spinner);
        int pos = spinner.getSelectedItemPosition();
        return pos;
    }

    private void updateSuperHero() {
        int iconHeight = 128;
        int iconWidth = 128;
        int superNum = getSuperHeroNum();
        int resource = R.drawable.current_location;
        switch(superNum) {
            case 0:
                resource = R.drawable.iron_man;
                break;
            case 1:
                resource = R.drawable.spider_man;
                break;
            case 2:
                resource = R.drawable.hulk;
                break;
        }
        BitmapDrawable originalIcon = (BitmapDrawable)getResources().getDrawable(resource);
        Bitmap b = originalIcon.getBitmap();
        Bitmap smallIcon = Bitmap.createScaledBitmap(b, iconWidth, iconHeight, false);
        if(currentLocation!=null)
            currentLocation.setIcon(BitmapDescriptorFactory.fromBitmap(smallIcon));
        if((superNum != currentSuperNum)&&(chosenVantagePointId!=null)) {
            navigate(chosenVantagePointId);
            currentSuperNum = superNum;
        }

    }

    private void addHeatMap(ArrayList<WeightedLatLng> list) {
        if (mOverlay != null)
            mOverlay.clearTileCache();
        if (list.size() > 0) {
            mProvider = new HeatmapTileProvider.Builder()
                    .weightedData(list)
                    .radius(50)
                    .build();

            mOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
        }
    }

    private int getResourceForCrime(String type) {
        switch(type) {
            case "Violence and sexual offences":
                return R.drawable.violent1;
            case "Anti-social behaviour":
                return R.drawable.asbo1;
            case "Burglary":
                return R.drawable.burglary1;
            case "Criminal damage and arson":
                return R.drawable.arson1;
            case "Other theft":
                return R.drawable.shoplift1;
            case "Possession of weapons":
                return R.drawable.weapons1;
            case "Robbery":
                return R.drawable.robbery1;
            case "Theft from the person":
                return R.drawable.robbery1;
            case "Vehicle crime":
                return R.drawable.vehicle1;
            case "Other crime":
                return R.drawable.robbery1;
            case "Public order":
                return R.drawable.publicorder1;
            case "Shoplifting":
                return R.drawable.shoplift1;
            case "Drugs":
                return R.drawable.drugs1;
            case "Bicycle theft":
                return R.drawable.bicycle1;
        }
        return R.drawable.crime;
    }
}
