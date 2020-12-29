package uk.co.firstchoice_cs.more;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import uk.co.firstchoice_cs.firstchoice.R;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.content.Context.LOCATION_SERVICE;

//ref http://www.zoftino.com/android-mapview-tutorial
public class OurLocationFragment extends Fragment implements OnMapReadyCallback {

    private static final String ARG_PARAM_TITLE = "title";
    private static final String MAP_VIEW_BUNDLE_KEY = "AIzaSyAE__q9Wb74fHR6Hcs1qxSiG5uQ1Sizgak";
    private static final int PERMISSION_REQUEST_CODE = 200;
    private double latitude = 52.671246;
    private double longitude = -2.002997;
    private MapView mapView;
    private GoogleMap gmap;

    public OurLocationFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View mView = inflater.inflate(R.layout.our_location_fragment, container, false);
        Button directionsBtn = mView.findViewById(R.id.directionsButton);

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                NavHostFragment.findNavController(OurLocationFragment.this).navigateUp();
            }
        };

        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);


        directionsBtn.setOnClickListener(view -> openDirectionsIntent());
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);
        }

        mapView = mView.findViewById(R.id.map_view);
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);
        return mView;
    }


    private void openDirectionsIntent() {
        if (checkPermission()) {
            try {

                gmap.setMyLocationEnabled(true);
                LocationManager service = (LocationManager) requireActivity().getSystemService(LOCATION_SERVICE);
                Criteria criteria = new Criteria();
                String provider = service.getBestProvider(criteria, false);
                Location location = service.getLastKnownLocation(provider);
                String url = "http://maps.google.com/maps?&daddr="
                        + latitude
                        + ","
                        + longitude;
                if (location != null) {
                    url = "http://maps.google.com/maps?saddr="
                            + location.getLatitude()
                            + ","
                            + location.getLongitude()
                            + "&daddr="
                            + latitude
                            + ","
                            + longitude;
                }


                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            } catch (SecurityException ex) {
                //if we failurePasswordResetCallBack that's step down gracefully
                openDirectionsDeniedIntent();
            }
        } else {
            requestPermission();
        }
    }

    private void openDirectionsDeniedIntent() {
        String url = "http://maps.google.com/maps?&daddr="
                + latitude
                + ","
                + longitude;

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }


    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        gmap = googleMap;
        LatLng fc = new LatLng(latitude, longitude);
        gmap.addMarker(new MarkerOptions().position(fc).title("First Choice Catering Ltd"));
        gmap.moveCamera(CameraUpdateFactory.newLatLng(fc));
        gmap.setMinZoomPreference(15);
    }

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(requireActivity().getApplicationContext(), ACCESS_FINE_LOCATION);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        requestPermissions(new String[]{ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0) {

                boolean locationAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                if (locationAccepted) {
                    openDirectionsIntent();
                } else {
                    openDirectionsDeniedIntent();
                    Toast.makeText(this.getContext(), "Permission Denied, You cannot access location documentItem.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}
