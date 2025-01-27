package com.example.ridesharing;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class DriverRideActivity extends AppCompatActivity {

    private MapView driverMapView;
    private Button acceptRideButton;
    private TextView rideDetailsTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_ride);

        // Set up OSM configurations
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        // Initialize the MapView
        driverMapView = findViewById(R.id.driverMapView);
        driverMapView.setBuiltInZoomControls(true);
        driverMapView.setMultiTouchControls(true);

        // Initialize ride details TextView
        rideDetailsTextView = findViewById(R.id.rideDetailsTextView);

        // Retrieve ride details from intent
        String rideDetails = getIntent().getStringExtra("rideDetails");
        rideDetailsTextView.setText(rideDetails);

        // Example pickup and drop-off points (replace with real data from intent)
        GeoPoint pickupPoint = new GeoPoint(27.7172, 85.3240); // Example: Kathmandu
        GeoPoint dropOffPoint = new GeoPoint(27.6727, 85.3188); // Example: Lalitpur

        // Set default view to the pickup location
        driverMapView.getController().setZoom(12.0);
        driverMapView.getController().setCenter(pickupPoint);

        // Add markers for pickup and drop-off locations
        addMarkerToMap(driverMapView, pickupPoint, "Pickup Location");
        addMarkerToMap(driverMapView, dropOffPoint, "Drop-off Location");

        // Initialize the Accept Ride button
        acceptRideButton = findViewById(R.id.acceptRideButton);
        acceptRideButton.setOnClickListener(v -> {
            // Accept the ride and provide feedback
            Toast.makeText(DriverRideActivity.this, "Ride Accepted: " + rideDetails, Toast.LENGTH_LONG).show();

            // Optionally, send this acceptance to the backend
            sendRideAcceptanceToBackend(rideDetails);

            // Navigate back to the dashboard or next screen
            finish();
        });
    }

    private void addMarkerToMap(MapView mapView, GeoPoint point, String title) {
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setTitle(title);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(marker);
        mapView.invalidate();
    }

    private void sendRideAcceptanceToBackend(String rideDetails) {
        // Simulate sending the ride acceptance to the backend
        // Replace this with actual API integration logic
        // Example:
        /*
        Call<BaseResponse<Void>> call = api.acceptRide(new RideAcceptanceRequest(driverId, rideDetails));
        call.enqueue(new Callback<BaseResponse<Void>>() {
            @Override
            public void onResponse(Call<BaseResponse<Void>> call, Response<BaseResponse<Void>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(DriverRideActivity.this, "Ride acceptance sent successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(DriverRideActivity.this, "Failed to send ride acceptance.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<Void>> call, Throwable t) {
                Toast.makeText(DriverRideActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        */
        Toast.makeText(this, "Ride acceptance sent to backend (simulated)", Toast.LENGTH_SHORT).show();
    }
}
