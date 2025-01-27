package com.example.ridesharing;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ridesharing.api.ApiClient;
import com.example.ridesharing.api.LoginApi;
import com.example.ridesharing.dto.BaseResponse;
import com.example.ridesharing.dto.LoginResponse;
import com.example.ridesharing.dto.RideRequestDTO;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapActivity extends AppCompatActivity {


    private MapView mapView;
    private AutoCompleteTextView pickupLocationEditText, dropOffLocationEditText;
    private Button requestRideButton;
    private Marker pickupMarker, dropOffMarker;
    private Spinner modeSpinner;  // Spinner for selecting transport mode

    private TextView distanceTextView;  // Add reference to the distance TextView
    private LoginApi loginApi;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up OSM configurations
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        setContentView(R.layout.activity_map);

        // Initialize the MapView
        mapView = findViewById(R.id.map);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);

        // Center between Kathmandu and Lalitpur
        GeoPoint centerPoint = new GeoPoint(27.6850, 85.3240); // Adjusted center coordinates
        mapView.getController().setZoom(17.0); // Zoom out slightly to include surrounding areas
        mapView.getController().setCenter(centerPoint);

        // Initialize form elements
        AutoCompleteTextView pickupLocationEditText = findViewById(R.id.pickupLocation);
        dropOffLocationEditText = findViewById(R.id.dropOffLocation);
        requestRideButton = findViewById(R.id.requestRideButton);
        modeSpinner = findViewById(R.id.modeSpinner);
        String[] transportModes = {"Walking", "Car", "Bike"};

// Create an ArrayAdapter for the Spinner
        ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, transportModes);
        modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

// Attach the adapter to the Spinner
        modeSpinner.setAdapter(modeAdapter);// Initialize mode spinner
        distanceTextView = findViewById(R.id.distanceTextView); // Initialize distance TextView

        // Add text watchers to trigger autocomplete search
        pickupLocationEditText.addTextChangedListener(new LocationTextWatcher(pickupLocationEditText, true));
        dropOffLocationEditText.addTextChangedListener(new LocationTextWatcher(dropOffLocationEditText, false));

        // Set click listener for Request Ride button
        requestRideButton.setOnClickListener(v -> {
            if (pickupMarker == null || dropOffMarker == null) {
                Toast.makeText(MapActivity.this, "Please select both pickup and drop-off locations.", Toast.LENGTH_SHORT).show();
            } else {
                // Get selected transport mode from spinner
                String selectedMode = modeSpinner.getSelectedItem().toString();

                // Draw the route
                drawRoute(pickupMarker.getPosition(), dropOffMarker.getPosition(), selectedMode);

                // Prepare the RideRequestDTO
                SharedPreferences sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE);
                String riderName = sharedPreferences.getString("name", "DefaultRider");
                Long riderId = sharedPreferences.getLong("riderId", 0);

                RideRequestDTO rideRequestDTO = new RideRequestDTO();
                rideRequestDTO.setRiderId(riderId); // Set the rider's ID
                rideRequestDTO.setRiderName(riderName); // Set the rider's name
                rideRequestDTO.setStartLocationName(pickupLocationEditText.getText().toString());
                rideRequestDTO.setEndLocationName(dropOffLocationEditText.getText().toString());
                rideRequestDTO.setStartLocationLatitude(String.valueOf(pickupMarker.getPosition().getLatitude()));
                rideRequestDTO.setStartLocationLongitude(String.valueOf(pickupMarker.getPosition().getLongitude()));
                rideRequestDTO.setEndLocationLatitude(String.valueOf(dropOffMarker.getPosition().getLatitude()));
                rideRequestDTO.setEndLocationLongitude(String.valueOf(dropOffMarker.getPosition().getLongitude()));
                rideRequestDTO.setAmount(calculateFare(pickupMarker.getPosition(), dropOffMarker.getPosition()));
                rideRequestDTO.setDistance(distanceTextView.getText().toString());

                // Make the API call
                loginApi = ApiClient.getClient(MapActivity.this).create(LoginApi.class);

                Call<BaseResponse<RideRequestDTO>> call = loginApi.create(rideRequestDTO);

                call.enqueue(new Callback<BaseResponse<RideRequestDTO>>() {
                    @Override
                    public void onResponse(Call<BaseResponse<RideRequestDTO>> call, Response<BaseResponse<RideRequestDTO>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Toast.makeText(MapActivity.this, "Ride request sent successfully!", Toast.LENGTH_SHORT).show();
                            Log.d("RideRequest", "Request saved successfully: " + response.body().toString());
                        } else {
                            Toast.makeText(MapActivity.this, "Failed to send ride request.", Toast.LENGTH_SHORT).show();
                            Log.e("RideRequest", "Failed with response code: " + response.code());
                            Log.e("RideRequest", "Response error: " + response.errorBody());
                            Toast.makeText(MapActivity.this, "Failed to send ride request: " + response.message(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<BaseResponse<RideRequestDTO>> call, Throwable t) {
                        Toast.makeText(MapActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("RideRequest", "API call failed", t);
                    }
                });
            }
        });

    }

    // TextWatcher to trigger Nominatim API for address suggestions
    private class LocationTextWatcher implements TextWatcher {
        private AutoCompleteTextView editText;
        private boolean isPickup;

        public LocationTextWatcher(AutoCompleteTextView editText, boolean isPickup) {
            this.editText = editText;
            this.isPickup = isPickup;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            String query = s.toString();
            if (query.length() > 2) { // Start searching if input length is greater than 2
                new NominatimSearchTask(editText, isPickup).execute(query);
            }
        }
    }

    // AsyncTask to perform Nominatim search for address suggestions
    private class NominatimSearchTask extends AsyncTask<String, Void, List<String>> {
        private AutoCompleteTextView autoCompleteTextView;
        private boolean isPickup;

        public NominatimSearchTask(AutoCompleteTextView autoCompleteTextView, boolean isPickup) {
            this.autoCompleteTextView = autoCompleteTextView;
            this.isPickup = isPickup;
        }

        @Override
        protected List<String> doInBackground(String... params) {
            String query = params[0];
            // Bounding box for Kathmandu Valley (corrected coordinates)
            String urlString = "https://nominatim.openstreetmap.org/search?format=json&q=" + query +
                    "&addressdetails=1&limit=5&countrycodes=np" +
                    "&viewbox=85.256538,27.861617,85.556915,27.573528&bounded=1";

            Log.d("NominatimSearchTask", "URL: " + urlString); // Debug: Log the URL being requested

            try {
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JSONArray jsonArray = new JSONArray(response.toString());
                List<String> suggestions = new ArrayList<>();

                Log.d("NominatimSearchTask", "Response: " + response); // Debug: Log the full API response

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String displayName = jsonObject.getString("display_name");
                    double lat = jsonObject.getDouble("lat");
                    double lon = jsonObject.getDouble("lon");

                    // Additional filtering based on latitude and longitude (optional)
                    if (lat >= 27.573528 && lat <= 27.861617 && lon >= 85.256538 && lon <= 85.556915) {
                        suggestions.add(displayName);
                    }
                }

                return suggestions;

            } catch (Exception e) {
                Log.e("NominatimSearchTask", "Error in autocomplete search", e);
                return null;
            }
        }


        @Override
        protected void onPostExecute(List<String> suggestions) {
            if (suggestions != null) {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(MapActivity.this,
                        android.R.layout.simple_dropdown_item_1line, suggestions);
                autoCompleteTextView.setAdapter(adapter);
                autoCompleteTextView.showDropDown();

                autoCompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
                    String selectedAddress = suggestions.get(position);
                    new NominatimGeocodeTask(selectedAddress, isPickup).execute();
                });
            } else {
                Toast.makeText(MapActivity.this, "Unable to retrieve suggestions. Please try again.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // AsyncTask to get coordinates for the selected address
    private class NominatimGeocodeTask extends AsyncTask<Void, Void, GeoPoint> {
        private String address;
        private boolean isPickup;

        public NominatimGeocodeTask(String address, boolean isPickup) {
            this.address = address;
            this.isPickup = isPickup;
        }

        @Override
        protected GeoPoint doInBackground(Void... voids) {
            String urlString = "https://nominatim.openstreetmap.org/search?format=json&q=" + address + "&limit=1&countrycodes=np";

            try {
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JSONArray jsonArray = new JSONArray(response.toString());
                if (jsonArray.length() > 0) {
                    JSONObject jsonObject = jsonArray.getJSONObject(0);
                    double lat = jsonObject.getDouble("lat");
                    double lon = jsonObject.getDouble("lon");
                    return new GeoPoint(lat, lon);
                }

            } catch (Exception e) {
                Log.e("NominatimGeocodeTask", "Error in geocoding", e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(GeoPoint point) {
            if (point != null) {
                if (isPickup) {
                    if (pickupMarker != null) mapView.getOverlays().remove(pickupMarker);
                    pickupMarker = new Marker(mapView);
                    pickupMarker.setPosition(point);
                    pickupMarker.setTitle("Pickup Location");
                    mapView.getOverlays().add(pickupMarker);
                } else {
                    if (dropOffMarker != null) mapView.getOverlays().remove(dropOffMarker);
                    dropOffMarker = new Marker(mapView);
                    dropOffMarker.setPosition(point);
                    dropOffMarker.setTitle("Drop-off Location");
                    mapView.getOverlays().add(dropOffMarker);
                }
                mapView.invalidate();
            } else {
                Toast.makeText(MapActivity.this, "Unable to retrieve coordinates.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Method to draw route using OSRM API
    private void drawRoute(GeoPoint pickupPoint, GeoPoint dropOffPoint, String mode) {
        // Calculate distance
        double distanceInKm = pickupPoint.distanceToAsDouble(dropOffPoint) / 1000; // Convert to kilometers
        distanceTextView.setText("Distance: " + String.format("%.2f", distanceInKm) + " km");  // Display distance


        // Determine the profile based on the mode
        String routeProfile = "driving"; // Default mode
        if (mode.equals("Walking")) {
            routeProfile = "walking";
        } else if (mode.equals("Bike")) {
            routeProfile = "cycling";
        }

        // Construct the OSRM API URL
        String urlString = "https://router.project-osrm.org/route/v1/" + routeProfile + "/"
                + pickupPoint.getLongitude() + "," + pickupPoint.getLatitude() + ";"
                + dropOffPoint.getLongitude() + "," + dropOffPoint.getLatitude()
                + "?overview=full&geometries=polyline&alternatives=false";


        Log.d("OSRM", "Fetching route with URL: " + urlString);

        new AsyncTask<String, Void, List<GeoPoint>>() {
            @Override
            protected List<GeoPoint> doInBackground(String... urls) {
                try {
                    URL url = new URL(urls[0]);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0");

                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    // Add this log to verify the response
                    Log.d("OSRM_Response", response.toString());

                    JSONObject json = new JSONObject(response.toString());
                    JSONArray routes = json.getJSONArray("routes");

                    if (routes.length() > 0) {
                        String polyline = routes.getJSONObject(0).getString("geometry");
                        return decodePolyline(polyline);
                    } else {
                        Log.e("OSRM", "No routes found in response");
                        return null;
                    }

                } catch (Exception e) {
                    Log.e("OSRM", "Error fetching route", e);
                    return null;
                }
            }


            @Override
            protected void onPostExecute(List<GeoPoint> routePoints) {
                if (routePoints != null) {
                    Polyline polyline = new Polyline();
                    polyline.setPoints(routePoints);

                    // Different color for each mode
                    if (mode.equals("Walking")) {
                        polyline.setColor(0xFF00FF00); // Green for walking
                    } else if (mode.equals("Bike")) {
                        polyline.setColor(0xFFFFA500); // Orange for bike
                    } else {
                        polyline.setColor(0xFF0000FF); // Blue for driving
                    }

                    mapView.getOverlayManager().add(polyline);
                    mapView.invalidate();

                    // Calculate the fare
                    double fare = calculateFare(pickupPoint, dropOffPoint);

                    // Update the fare TextView
                    TextView fareTextView = findViewById(R.id.fareTextView);
                    fareTextView.setText("Total Fare: NPR " + String.format("%.2f", fare));
                } else {
                    Toast.makeText(MapActivity.this, "Failed to retrieve route.", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute(urlString);


        SharedPreferences sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE);

        String userName = sharedPreferences.getString("userName", "defaultUsername");
        String userToken = sharedPreferences.getString("userToken", "defaultToken");
        String userType = sharedPreferences.getString("userType", "defaultUserType");
        String riderName =  sharedPreferences.getString("name","rider");
        String userId = sharedPreferences.getString("name","rider");
//        String[] pickupPoints = pickupPoint.split(",");
//        String[] dropOffPoint = dropO.s

        /*String userId = sharedPreferences.getString("userId", "defaultUserId");
        String name = sharedPreferences.getString("name", "defaultName");
*/
        RideRequestDTO rideRequestDTO = new RideRequestDTO();
        // Setting default values for the fields
        rideRequestDTO.setRiderId(1l); // Default rider ID as 0
        rideRequestDTO.setRiderName(riderName); // Default name
        rideRequestDTO.setAmount(calculateFare(pickupPoint, dropOffPoint)); // Default amount
        rideRequestDTO.setStartLocationName(""); // Default start location
        rideRequestDTO.setEndLocationName("Not Available"); // Default end location
        rideRequestDTO.setStartLocationLatitude(String.valueOf(pickupPoint.getLatitude())); // Default latitude
        rideRequestDTO.setStartLocationLongitude(String.valueOf(pickupPoint.getLongitude())); // Default longitude
        rideRequestDTO.setEndLocationLatitude(String.valueOf(dropOffPoint.getLatitude())); // Default latitude
        rideRequestDTO.setEndLocationLongitude(String.valueOf(dropOffPoint.getLongitude())); // Default longitude
        rideRequestDTO.setDistance(String.valueOf(distanceInKm));


        loginApi = ApiClient.getClient(MapActivity.this).create(LoginApi.class);



        Call<BaseResponse<RideRequestDTO>> call = loginApi.create(rideRequestDTO);

        call.enqueue(new Callback<BaseResponse<RideRequestDTO>>() {
            @Override
            public void onResponse(Call<BaseResponse<RideRequestDTO>> call, Response<BaseResponse<RideRequestDTO>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    System.out.println("saved successfully");
                    //handleLoginResponse(response.body().getObj());
                } else {
                    //showToast("Login failed: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<BaseResponse<RideRequestDTO>> call, Throwable t) {

            }

        });
    }

    private double calculateFare(GeoPoint pickupPoint, GeoPoint dropOffPoint) {
        // Constants for fare calculation
        final double BASE_FARE = 50;  // NPR
        final double PER_KM_FARE = 15;  // NPR
        final double PER_MIN_FARE = 3;  // NPR
        final double SURGE_MULTIPLIER = 1.5;  // Surge pricing multiplier
        final double ADDITIONAL_CHARGES = 20;  // Waiting time or other charges

        // Calculate the distance between pickup and drop-off points in kilometers
        double distanceInKm = pickupPoint.distanceToAsDouble(dropOffPoint) / 1000;

        // Estimate the travel time (in minutes) - simple estimation based on distance
        double estimatedTimeInMinutes = distanceInKm * 2; // assuming average speed of 30 km/h

        // Calculate the fare components
        double distanceFare = distanceInKm * PER_KM_FARE;
        double timeFare = estimatedTimeInMinutes * PER_MIN_FARE;

        // Apply surge pricing (this can be modified based on your app's demand logic)
        double surgePrice = BASE_FARE + distanceFare + timeFare + ADDITIONAL_CHARGES;
        surgePrice *= SURGE_MULTIPLIER;

        // Calculate total fare
        double totalFare = BASE_FARE + distanceFare + timeFare + ADDITIONAL_CHARGES;

        // Return the total fare
        return totalFare;
    }



    private List<GeoPoint> decodePolyline(String encoded) {
        List<GeoPoint> geoPoints = new ArrayList<>();
        int index = 0, lat = 0, lng = 0;
        while (index < encoded.length()) {
            int shift = 0, result = 0, b;
            do { b = encoded.charAt(index++) - 63; result |= (b & 0x1f) << shift; shift += 5; } while (b >= 0x20);
            lat += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);
            shift = result = 0;
            do { b = encoded.charAt(index++) - 63; result |= (b & 0x1f) << shift; shift += 5; } while (b >= 0x20);
            lng += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);
            geoPoints.add(new GeoPoint(lat / 1E5, lng / 1E5));
        }
        return geoPoints;
    }
}
