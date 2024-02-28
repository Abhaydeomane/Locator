package com.example.locator;

import static java.security.AccessController.getContext;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import androidx.core.util.Pair;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationServices;
import android.location.Location;
import android.location.LocationManager;
import android.annotation.SuppressLint;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Firebase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import android.app.AlertDialog;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


public class MainActivity extends AppCompatActivity {

    FirebaseAuth auth;
    Button btn_logout,btn_profile;
    TextView textViewEmail,textViewName,latitudeTextView, longitTextView;
    int PERMISSION_ID = 44;

    int search_distance=15;
    FirebaseUser user;
    FirebaseDatabase db=FirebaseDatabase.getInstance();
    DatabaseReference reference;

    HashMap<String, Users> usersMap = new HashMap<>();

    double latitude,longitude;
    private ListView mListView;
    ListView l;
    String tutorials[]
            = { "Algorithms", "Data Structures",
            "Languages", "Interview Corner",
            "GATE", "ISRO CS",
            "UGC NET CS", "CS Subjects",
            "Web Technologies" };


    // initializing
    // FusedLocationProviderClient
    // object
    FusedLocationProviderClient mFusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth=FirebaseAuth.getInstance();
        btn_logout=findViewById(R.id.logout);
        btn_profile=findViewById(R.id.Profile);
        textViewEmail=findViewById(R.id.user_email);
        textViewName=findViewById(R.id.user_name);
        latitudeTextView = findViewById(R.id.latTextView);
        longitTextView = findViewById(R.id.lonTextView);
        user=auth.getCurrentUser();
        mListView = findViewById(R.id.list_view);
        l = findViewById(R.id.list_view);


        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


        if(user==null){
            Intent intent=new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        }

        textViewEmail.setText(user.getEmail());
        textViewName.setText(user.getDisplayName());
        // method to get the location
        getLastLocation();

        getonlineuser();


        btn_logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                Intent intent=new Intent(getApplicationContext(), Login.class);
                startActivity(intent);
                finish();
            }
        });
        btn_profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(MainActivity.this, ProfileUpdate.class);
                intent.putExtra("User_uid", user.getUid());
                intent.putExtra("User_email", user.getEmail());
                startActivity(intent);
            }
        });

    }

    @Override
    protected void onDestroy() {
        // Get current user's UID
        // Remove the user from onlineusers node to indicate offline status
        reference = db.getReference("Onlineusers");
        reference.child(user.getUid()).removeValue();
        super.onDestroy();
    }

    private void getonlineuser(){
        reference = db.getReference("Onlineusers");
        AtomicInteger usersWithinRangeCount = new AtomicInteger(0); // Initialize an AtomicInteger
        reference.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    Log.e("firebase", "Error getting data", task.getException());
                }
                else {
                    DataSnapshot dataSnapshot = task.getResult();
                    if (dataSnapshot != null) {
                        for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                            String userId = userSnapshot.getKey();
                            if (userId != null) {
                                Double latitude_temp = userSnapshot.child("latitude").getValue(Double.class);
                                Double longitude_temp = userSnapshot.child("longitude").getValue(Double.class);

                                if (latitude_temp != null && longitude_temp != null) {
                                    // Calculate the distance between the two points
                                    double distance = calculateDistance(latitude, longitude, latitude_temp, longitude_temp);
                                    // If the distance is less than 15 meters, add the userdata to usersMap
                                    if (distance < 0.015) { // 0.015 represents 15 meters (as latitude and longitude are in degrees)
                                        usersWithinRangeCount.incrementAndGet(); // Increment the count
                                        FirebaseDatabase db=FirebaseDatabase.getInstance();
                                        reference = db.getReference("Users");//extracting userdata from database
                                        reference.child(userId).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<DataSnapshot> task) {
                                                if (!task.isSuccessful()) {
                                                    Log.e("firebase", "Error getting data", task.getException());
                                                }
                                                else {
                                                    //Toast.makeText(MainActivity.this, "done step2", Toast.LENGTH_SHORT).show();
                                                    DataSnapshot dataSnapshot_temp_users = task.getResult();
                                                    String firstName_temp_user = dataSnapshot_temp_users.child("firstName").getValue(String.class);
                                                    String lastName_temp_user = dataSnapshot_temp_users.child("lastName").getValue(String.class);
                                                    String mobile_temp_user = dataSnapshot_temp_users.child("mobile").getValue(String.class);
                                                    String email_temp_user = dataSnapshot_temp_users.child("mobile").getValue(String.class);

                                                    Users user_temp = new Users(firstName_temp_user, lastName_temp_user, email_temp_user, mobile_temp_user, latitude_temp,longitude_temp);
                                                    usersMap.put(userId, user_temp);


                                                    // Check if usersMap is populated and then display the list
                                                    // +1 is for tempuser
                                                    if (usersMap.size() == usersWithinRangeCount.get()+1) {
                                                        displayUserList();
                                                    }
                                                }
                                            }
                                        });

                                        Users user_temp = new Users("john", "Doe", "email", "4321423", 332.23,32.12);
                                        usersMap.put("tempuser", user_temp);

                                    }
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        // check if permissions are given
        if (checkPermissions()) {

            // check if location is enabled
            if (isLocationEnabled()) {

                // getting last// location from
                // FusedLocationClien// object
                mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        Location location = task.getResult();
                        if (location == null) {
                            requestNewLocationData();
                        } else {
                            latitude=location.getLatitude();
                            longitude=location.getLongitude();
                            latitudeTextView.setText(latitude+ "");
                            longitTextView.setText(longitude + "");

                            //update in database
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("latitude", latitude);
                            updates.put("longitude", longitude);
                            reference=db.getReference("Onlineusers");
                            reference.child(user.getUid()).updateChildren(updates)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            //Toast.makeText(MainActivity.this, "Successfully Update in DB", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(MainActivity.this, "Failed to update", Toast.LENGTH_SHORT).show();
                                        }
                                    });

                        }
                    }
                });
            } else {
                Toast.makeText(this, "Please turn on" + " your location...", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            // if permissions aren't available,// request for permissions
            requestPermissions();
        }
    }

    @SuppressLint("MissingPermission")
    private void requestNewLocationData() {

        // Initializing LocationRequest// object with appropriate methods
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(5000);//interval for location updates in milliseconds. here 5 sec
        mLocationRequest.setFastestInterval(0);
        mLocationRequest.setNumUpdates(1);//here number of times the update location will works . increase the count in future

        // setting LocationRequest// on FusedLocationClient
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
    }

    private LocationCallback mLocationCallback = new LocationCallback() {

        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location mLastLocation = locationResult.getLastLocation();
            latitudeTextView.setText("Latitude: " + mLastLocation.getLatitude() + "");
            longitTextView.setText("Longitude: " + mLastLocation.getLongitude() + "");
        }
    };

    // method to check for permissions
    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        // If we want background location// on Android 10.0 and higher,
        // use:
        // ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    // method to request for permissions
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_ID);
    }

    // method to check
    // if location is enabled
    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    // If everything is alright then
    @Override
    public void
    onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (checkPermissions()) {
            getLastLocation();
        }
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // Radius of the earth in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // Convert to meters
        return 0;
        //change return value to zero
    }

    // Method to display the list of users after the usersMap has been populated
    private void displayUserList() {
        Toast.makeText(MainActivity.this, "Fetched online users successfully", Toast.LENGTH_SHORT).show();

        ArrayList<String> usersList = new ArrayList<>();

        for (String userId : usersMap.keySet()) {
            Users user_temp = usersMap.get(userId);
            String userInfo = user_temp.getFirstName() + " " + user_temp.getLastName();
            usersList.add(userInfo);
        }

        ArrayAdapter<String> arr = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_dropdown_item, usersList);
        l.setAdapter(arr);

        // Set OnItemClickListener for the ListView
        l.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Retrieve the selected item from the adapter
                String selectedItem = (String) parent.getItemAtPosition(position);

                // Find the corresponding key in the usersMap
                String userId = getUserIdFromSelectedItem(selectedItem);

                // Get the user details based on the userId
                Users selectedUser = usersMap.get(userId);

//                 Example: You can display extra details using a dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("User Details");
//                // Inflate the custom layout for the dialog
//                View dialogView = getLayoutInflater().inflate(R.layout.dialog_user_details, null);
//                builder.setView(dialogView);
//
//                // Access views in the custom layout
//                ImageView profileImageView = dialogView.findViewById(R.id.profile_image);
//                TextView nameTextView = dialogView.findViewById(R.id.name_text_view);
//                TextView detailsTextView = dialogView.findViewById(R.id.details_text_view);
//
//                if (selectedUser != null) {
//                    // Set user details
//                    nameTextView.setText(selectedUser.getFirstName() + " " + selectedUser.getLastName());
//                    detailsTextView.setText("Other details: Add your extra details here");

                    // Load profile picture into the ImageView using Picasso or Glide
                    // Example using Picasso:
//                    Picasso.get().load(selectedUser.getProfilePictureUrl()).into(profileImageView);
//                } else {
//                    nameTextView.setText("User details not found.");
//                }
//
//                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        dialog.dismiss();
//                    }
//                });
//                builder.show();
                //simple design
                if (selectedUser != null) {
                    builder.setMessage("Name: " + selectedUser.getFirstName() + " " + selectedUser.getLastName() + "\n"
                            + "Other details: Add your extra details here");
                } else {
                    builder.setMessage("User details not found.");
                }
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.show();
            }
        });
    }
    private String getUserIdFromSelectedItem(String selectedItem) {
        // Iterate through usersMap to find the corresponding userId
        for (Map.Entry<String, Users> entry : usersMap.entrySet()) {
            Users user = entry.getValue();
            String userInfo = user.getFirstName() + " " + user.getLastName();
            if (userInfo.equals(selectedItem)) {
                return entry.getKey();
            }
        }
        return null; // Return null if userId not found
    }


}