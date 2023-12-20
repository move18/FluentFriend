package com.example.fluentfriend;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.net.*;

import java.util.*;
import java.util.stream.Collectors;

import static com.example.fluentfriend.MatchPage.getActiveUser;

public class LocationSuggestion extends AppCompatActivity {

    private static final String API_KEY = BuildConfig.API_KEY;
    private static final String TAG = "NearbyPlacesDebug";
    private PlacesClient placesClient;
    private Location midpoint;
    private User user1;
    private User user2;
    private UserLocation user1Location;
    private UserLocation user2Location;
    private double user1lat;
    private double user1long;
    private double user2lat;
    private double user2long;
    private TextView resultView;
    private String query = "";
    private List<String> commonInterests = new ArrayList<>();
    private List<String> commonInterestsAPIFormat = new ArrayList<>();
    private StringBuilder queryBuilder = new StringBuilder();

    private static final int LOCATION_RADIUS_DISTANCE_METERS = 1000;
    Locale swedenLocale = new Locale("sv", "SE");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_suggestion);

        //gets the two User objects for the two users that have matched /G
        this.user1 = UserManager.getCurrentUser();
        Intent intent = getIntent();
        this.user2 = (User) intent.getSerializableExtra("matchedUser");

        //saves the UserLocation object for the two users that have matched (they are in the DB active list) /G
        user1Location = getActiveUser(user1);
        user2Location = getActiveUser(user2);

        //calculates the middle point between the two users based on their respective locations /G
        midpoint = getMiddleDistanceBetweenUsers();

        // Initialize the Places SDK
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), API_KEY);
        }
        placesClient = Places.createClient(this);

        findCommonInterests();
        displayCommonInterests();

        resultView = findViewById(R.id.resultTextView);

        //button that runs the method for finding nearby places /G
        Button findNearbyPlacesButton = findViewById(R.id.find_nearby_places);
        findNearbyPlacesButton.setOnClickListener(view -> {
            fetchNearbyPlaces();
        });

        //button that leaves the app and opens Google Maps with a search query based on location and interests /G
        Button openGoogleMapsButton = findViewById(R.id.open_google_maps);
        openGoogleMapsButton.setOnClickListener(view -> openGoogleMaps(midpoint));
    }

    //these methods check what interests the users have in common /G
    private boolean doBothLikeFika() {
        return user1.isFikaChecked() && user2.isFikaChecked();
    }

    private boolean doBothLikeMuseum() {
        return user1.isMuseumChecked() && user2.isMuseumChecked();
    }

    private boolean doBothLikeBar() {
        return user1.isBarChecked() && user2.isBarChecked();
    }

    private boolean doBothLikeCityWalk() {
        return user1.isCityWalksChecked() && user2.isCityWalksChecked();
    }

    //displays the common interests in the textView /G
    private void displayCommonInterests() {
        String interestsText = "";
        if (!commonInterests.isEmpty()) {
            // Joins common interests in a single string separated by commas
            interestsText += String.join(", ", commonInterests);
        } else {
            // Handles no common interests
            interestsText = "You have no common interests :(";
        }
        TextView textViewCommonInterests = findViewById(R.id.CommonInterestsTextView); // Replace with your actual TextView ID
        textViewCommonInterests.setText(interestsText);

    }

    //finds the common interests of both users and adds them to a list of Strings that can be shown to the user,
    //as well as to a querybuilder that is formatted for Google search queries to be used by the Google Maps query /G
    private void findCommonInterests() {
        if (doBothLikeFika()) {
            commonInterests.add("Fika");
            commonInterestsAPIFormat.add("cafe");
            queryBuilder.append("Cafes");
        }
        if (doBothLikeMuseum()) {
            commonInterests.add("Museums");
            commonInterestsAPIFormat.add("museum");
            if (queryBuilder.length() > 0) queryBuilder.append(" or ");
            queryBuilder.append("Museums");
        }
        if (doBothLikeBar()) {
            commonInterests.add("Bars");
            commonInterestsAPIFormat.add("bar");
            if (queryBuilder.length() > 0) queryBuilder.append(" or ");
            queryBuilder.append("Bars");
        }
        if (doBothLikeCityWalk()) {
            commonInterests.add("City Walks");
        }
    }

    //finds the lat/long middle point between the two users, returns as Location object /G
    private Location getMiddleDistanceBetweenUsers() {
        double lat = (user1Location.getLatitude() + user2Location.getLatitude()) / 2;
        double longitude = (user1Location.getLongitude() + user2Location.getLongitude()) / 2;
        Location midpoint = new Location("");
        midpoint.setLatitude(lat);
        midpoint.setLongitude(longitude);
        return midpoint;
    }

    //When Google Maps button is pressed, Google Maps opens with a search for the common interests /G
    private void openGoogleMaps(Location location) {
        //sets the query depending on the common interests
        this.query = queryBuilder.toString();

        // Create a Uri from an intent string. Use the result to create an Intent.
        //write easier-to-understand comment what this does /G
        Uri gmmIntentUri = Uri.parse("geo:" + location.getLatitude() + "," + location.getLongitude() + "?q=" + Uri.encode(query));

        // Create an Intent to open Google Maps at the specified location
        //same here /G
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        // Attempt to start an activity that can handle the Intent
        //also here /G
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            // Display error text if Google Maps is not installed
            Toast.makeText(this, "Google Maps is not installed.", Toast.LENGTH_LONG).show();
        }
    }

    private void fetchNearbyPlaces() {

        for (String commonInterest : commonInterestsAPIFormat) {
            Log.d(TAG, "Your common interests are: " + commonInterest);
        }

        List<Place.Field> placeFields = Arrays.asList(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.TYPES,
                Place.Field.LAT_LNG);

        FindCurrentPlaceRequest request = FindCurrentPlaceRequest.newInstance(placeFields);

        if (ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                        this, android.Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
            // ActivityCompat#requestPermissions logic here...
            return;
        }
        Log.d(TAG, "Making Places API call...");
        Task<FindCurrentPlaceResponse> placeResponse = placesClient.findCurrentPlace(request);

        placeResponse.addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Log.d(TAG, "Places API call was successful.");

                FindCurrentPlaceResponse response = task.getResult();

                List<Place> nearbyPlaces = response.getPlaceLikelihoods().stream()
                        .map(PlaceLikelihood::getPlace)

                        .filter(place -> place.getLatLng() != null)

                        //this filter does not work, when enabled = no results in textview
                        //.filter(place -> isWithinRadius(place.getLatLng(), midpoint))

                        //only returns 1 result (1 cafe) for some reason
                        .filter(place -> place.getPlaceTypes() != null && place.getPlaceTypes().stream()
                                .anyMatch(typeString -> commonInterestsAPIFormat.contains(typeString.toLowerCase())))

                        //collects all the places that passed the filters into a list
                        .collect(Collectors.toList());

                //checks if any places were added to the list
                if (nearbyPlaces.isEmpty()) {
                    Log.d(TAG, "Nearby places list is empty after filtering");
                }

                if (!nearbyPlaces.isEmpty()) {
                    //Shows what places were added before sorting
                    for (Place place : nearbyPlaces) {
                        Log.d(TAG, "These are the places in the nearbyPlaces list before sorting");
                        Log.d(TAG, "Place found: " + place.getName() + " at " + place.getLatLng());
                    }
                }

                nearbyPlaces.sort(Comparator.comparing(place -> getDistanceFromMidpoint(place.getLatLng())));

                if (nearbyPlaces.isEmpty()) {
                    Log.d(TAG, "Nearby places list is still empty after being sorted");
                }

                if (!nearbyPlaces.isEmpty()) {
                    for (Place place : nearbyPlaces) {
                        Log.d(TAG, "The nearbyPlaces list is now sorted and contains:");
                        Log.d(TAG, "Place found: " + place.getName() + " at " + place.getLatLng());
                    }
                }

                List<Place> closestPlaces = nearbyPlaces.stream()
                        .limit(3)
                        .collect(Collectors.toList());

                for (Place place : closestPlaces) {
                    Log.d(TAG, "The 3 closest places have now been found and are: ");
                    Log.d(TAG, "Place found: " + place.getName() + " at " + place.getLatLng());
                }

                List<Task<FetchPlaceResponse>> fetchPlaceTasks = new ArrayList<>();
                for (Place place : closestPlaces) {
                    List<Place.Field> detailFields = Collections.singletonList(Place.Field.OPENING_HOURS);
                    FetchPlaceRequest detailRequest = FetchPlaceRequest.newInstance(place.getId(), detailFields);
                    fetchPlaceTasks.add(placesClient.fetchPlace(detailRequest));
                }

                Tasks.whenAllSuccess(fetchPlaceTasks).addOnSuccessListener(placesWithDetails -> {

                    StringBuilder suggestion = new StringBuilder();
                    for (Object responseObj : placesWithDetails) {
                        FetchPlaceResponse fetchPlaceResponse = (FetchPlaceResponse) responseObj;
                        Place detailedPlace = fetchPlaceResponse.getPlace();
                        String openingHoursText = detailedPlace.getOpeningHours() != null ? detailedPlace.getOpeningHours().toString() : "Opening hours not available";
                        suggestion.append(detailedPlace.getName()).append(" - ").append(openingHoursText).append("\n");
                    }
                    Log.d(TAG, "Data to be presented in TextView: " + suggestion);
                    runOnUiThread(() -> resultView.setText(suggestion.toString()));
                }).addOnFailureListener(exception -> {
                    Log.e(TAG, "Error fetching place details: " + exception.getMessage());
                });
            } else {
                Log.e(TAG, "Places API call failed " + task.getException());
            }
        }).addOnFailureListener((exception) -> {
            Log.e(TAG, "Places API call encountered an error: " + exception.getMessage());
        });
    }
    private boolean isWithinRadius(LatLng placeLatLng, Location midpoint) {
        float[] results = new float[1];
        Location.distanceBetween(midpoint.getLatitude(), midpoint.getLongitude(),
                placeLatLng.latitude, placeLatLng.longitude, results);
        return results[0] <= LocationSuggestion.LOCATION_RADIUS_DISTANCE_METERS;
    }
    private float getDistanceFromMidpoint(LatLng placeLatLng) {
        Location placeLocation = new Location(""); // provider is not needed here
        placeLocation.setLatitude(placeLatLng.latitude);
        placeLocation.setLongitude(placeLatLng.longitude);
        return getDistance(midpoint, placeLocation);
    }
    private float getDistance(Location originLocation, Location targetLocation) {
        float[] results = new float[1];
        Location.distanceBetween(originLocation.getLatitude(), originLocation.getLongitude(),
                targetLocation.getLatitude(), targetLocation.getLongitude(),
                results);
        return results[0]; // distance in meters
    }

    //method for formatting distance printout if needed
    private String formatDistance(float distance) {
        if (distance < 1000) {
            return String.format(swedenLocale, "%d meter", (int) distance);
        } else {
            return String.format(swedenLocale, "%.2f km", distance / 1000);
        }
    }
}