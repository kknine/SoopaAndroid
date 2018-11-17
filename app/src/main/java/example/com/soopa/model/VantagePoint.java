package example.com.soopa.model;

import com.google.android.gms.maps.model.LatLng;

public class VantagePoint {
    private final LatLng position;
    private final double height;
    public static final String SERVER_LAT = "lat";
    public static final String SERVER_LNG = "lng";
    public static final String SERVER_HEIGHT = "height";

    public VantagePoint(LatLng position, double height) {
        this.position = position;
        this.height = height;
    }

    public LatLng getPosition() {
        return position;
    }

    public double getHeight() {
        return height;
    }
}
