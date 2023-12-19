package com.example.fluentfriend;

import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.maps.GeoApiContext;
import com.google.maps.PlacesApi;
import com.google.maps.model.LatLng;
import com.google.maps.model.PlaceType;
import com.google.maps.model.PlacesSearchResult;

import java.util.*;
import java.util.stream.Collectors;

import static com.example.fluentfriend.MatchPage.getActiveUser;

public class LocationSuggestion extends AppCompatActivity {
    private static final String API_KEY = BuildConfig.API_KEY;
    private GeoApiContext context;
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

        //gets the lat/long values from the UserLocation object for the two users /G
        this.user1lat = user1Location.getLatitude();
        this.user1long = user1Location.getLongitude();
        this.user2lat = user2Location.getLatitude();
        this.user2long = user2Location.getLongitude();

        //calculates the middle point between the two users based on their respective locations /G
        midpoint = getMiddleDistanceBetweenUsers(user1lat, user1long, user2lat, user2long);

        //what does this do? /G
        context = new GeoApiContext.Builder()
                .apiKey(API_KEY)
                .build();

        //displays to the user what interests they have in common in a textview /G
        displayCommonInterests();

        //button that runs the method for finding nearby places /G
        Button findNearbyPlacesButton = findViewById(R.id.find_nearby_places);
        findNearbyPlacesButton.setOnClickListener(view -> {fetchNearbyPlaces(commonInterests);});

        //Displays results from fetchNearbyPlaces() /G
        resultView = findViewById(R.id.resultTextView);

        //button that leaves the app and opens Google Maps with a search query based on location and interests /G
        Button openGoogleMapsButton = findViewById(R.id.open_google_maps);
        openGoogleMapsButton.setOnClickListener(view -> openGoogleMaps(midpoint));

        //button for messaging the other user that you've matched with
        Button messageUserButton = findViewById(R.id.message_user_button);
        messageUserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessageToUser();
            }
        });
    }
    private void sendMessageToUser() { //this needs to be added when Firebasemessaging is up
        //code for message request to server if this is implemented
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
        findCommonInterests();
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
            queryBuilder.append("Cafes");
        }
        if (doBothLikeMuseum()) {
            commonInterests.add("Museums");
            if (queryBuilder.length() > 0) queryBuilder.append(" or ");
            queryBuilder.append("Museums");
        }
        if (doBothLikeBar()) {
            commonInterests.add("Bars");
            if (queryBuilder.length() > 0) queryBuilder.append(" or ");
            queryBuilder.append("Bars");
        }
        if (doBothLikeCityWalk()) {
            commonInterests.add("City Walks");
        }
    }

    //finds the lat/long middle point between the two users, returns as Location object /G
    private Location getMiddleDistanceBetweenUsers(double user1lat, double user1long, double user2lat, double user2long) {
        double lat = (user1lat + user2lat) / 2;
        double longitude = (user1long + user2long) / 2;
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

    private void fetchNearbyPlaces(List<String> interestTypes) {
        //explanation about threads
        new Thread(() -> {
            try {
                //for debugging
                Log.d("Before UI thread", "STARTING THREAD");

                LatLng location = new LatLng(midpoint.getLatitude(), midpoint.getLongitude());
                List<PlacesSearchResult> combinedResults = new ArrayList<>();

                for (String interestType : interestTypes) {
                    PlaceType placeType;
                    switch (interestType.toLowerCase()) {
                        case "bars":
                            placeType = PlaceType.BAR;
                            break;
                        case "museums":
                            placeType = PlaceType.MUSEUM;
                            break;
                        case "fika":
                            placeType = PlaceType.CAFE;
                            break;
                        default:
                            continue;
                    }

                    PlacesSearchResult[] results = PlacesApi.nearbySearchQuery(context, location)
                            .radius(LOCATION_RADIUS_DISTANCE_METERS) // distance in meters
                            .type(placeType)
                            .await()
                            .results;

                    Collections.addAll(combinedResults, results);
                }

                combinedResults.sort(Comparator.comparing(result -> getDistance(location, result.geometry.location)));

                List<PlacesSearchResult> closestResults = combinedResults.stream().limit(3).collect(Collectors.toList());

                for (PlacesSearchResult result : closestResults) {
                   // String placeId = result.placeId;
                    //String openingHours = fetchOpeningHours(placeId);
                }
                runOnUiThread(() -> {
                    if (!closestResults.isEmpty()) {
                        StringBuilder suggestion = new StringBuilder();
                        for (PlacesSearchResult result : closestResults) {
                            suggestion.append(result.name).append("\n");
                        }
                        resultView.setText(suggestion.toString());
                    } else {
                        Toast.makeText(this, "No nearby places found.", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String formatDistance(float distance) {
        if (distance < 1000) {
            return String.format(swedenLocale, "%d meter", (int) distance);
        } else {
            return String.format(swedenLocale, "%.2f km", distance / 1000);
        }
    }
    private float getDistance(LatLng currentLocation, LatLng placeLocation) {
        float[] results = new float[1];
        Location.distanceBetween(
                currentLocation.lat, currentLocation.lng,
                placeLocation.lat, placeLocation.lng,
                results);
        return results[0]; // distance in meters
    }
}