package sairaj.content.com.yelper;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.LayerDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.jakewharton.processphoenix.ProcessPhoenix;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class Home extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    final int LOCATION_REQUEST = 42;
    LocationRequest locationRequest;
    GoogleApiClient gapiClient;
    Location lastLocation;
    ArrayList<String> credentials;
    Marker marker;
    MapFragment mapFragment;
    double latitude = 0.0;
    double longitude = 0.0;
    ArrayList<ArrayList<String>> yelp_returns;
    SharedPreferences sharedPreferences;
    Boolean mapReady = false;
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().getExtras() != null && getIntent().getExtras().getBoolean("EXIT", false)) {
            finish();
        }
        mapReady = false;
        sharedPreferences = getSharedPreferences("Yelper", Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        imageView = (ImageView) findViewById(R.id.imageV);
        if (checkGPS(getApplicationContext())) {
            if (checkInternetConnection(getApplicationContext())) {
                setContentView(R.layout.activity_home);
                int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION);
                if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                            LOCATION_REQUEST);
                } else {
                    //http://www.mocky.io/v2/591e09733f00009b0b77c930 -- MOCK REST endpoint to access clientID and clientSecret
                    Utils.getProcessor getProcessor = new Utils.getProcessor(getApplicationContext(), new Utils.getProcessor.TaskListener() {

                        @Override
                        public void onFinished(ArrayList<String> result, Context context) {
                            if (result != null) {
                                credentials = result;
                                String clientID = credentials.get(0);
                                String clientSecret = credentials.get(1);
                                editor.putString("clientID", clientID);
                                editor.putString("clientSecret", clientSecret);
                                editor.commit();
                                googleClientServices();
                                getYelpAccessToken();

                            }

                        }

                        @Override
                        public void onComplete(ArrayList<ArrayList<String>> result, Context context) {

                        }
                    });
                    getProcessor.execute("http://www.mocky.io/v2/591e09733f00009b0b77c930", "0");

                }
            }
            else
            {
                noInternet();
            }

        }
        else
        {
            noGPS();
        }

    }
    public void noInternet()
    {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }

        builder.setTitle("No internet").setMessage("App cannot continue, please connect device to internet");
        builder.setPositiveButton("Retry", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(!checkInternetConnection(getApplicationContext()))
                {
                    dialog.cancel();
                    noInternet();
                }
                else
                {
                    dialog.cancel();
                    ProcessPhoenix.triggerRebirth(getApplicationContext());
                }

            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public void noGPS()
    {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }

        builder.setTitle("No GPS").setMessage("App cannot continue, please enable GPS");
        builder.setPositiveButton("Retry", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(!checkGPS(getApplicationContext()))
                {
                    dialog.cancel();
                    noGPS();
                }
                else
                {
                    dialog.cancel();
                    ProcessPhoenix.triggerRebirth(getApplicationContext());
                }

            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
    public boolean checkGPS(Context context) {
        boolean state;
        LocationManager locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        state = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        return state;
    }

    public boolean checkInternetConnection(Context context)
    {
        boolean state;
        ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        state = networkInfo != null && networkInfo.isConnectedOrConnecting();
        return state;
    }

    public void renderMap() {
        GoogleMapOptions googleMapOptions = new GoogleMapOptions();
        googleMapOptions.mapType(GoogleMap.MAP_TYPE_TERRAIN).compassEnabled(true);
        mapFragment = MapFragment.newInstance(googleMapOptions);
        FragmentTransaction fragmentTransaction =
                getFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.map, mapFragment);
        fragmentTransaction.commit();

    }

    public void googleClientServices() {
        createLocationRequest();
        gapiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        gapiClient.connect();
    }

    public void getYelpAccessToken() {
        Utils.postProcessor postProcessor = new Utils.postProcessor(getApplicationContext(), new Utils.postProcessor.TaskListener() {
            @Override
            public void onFinished(ArrayList<String> s, Context context) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("access_token", s.get(0));
                editor.commit();
                getRestaurants(s.get(0));


            }
        });

        postProcessor.execute(sharedPreferences.getString("clientID", ""), sharedPreferences.getString("clientSecret", ""), "https://api.yelp.com/oauth2/token");
    }

    public void getRestaurants(String access_token) {
        Utils.getProcessor getProcessor = new Utils.getProcessor(getApplicationContext(), new Utils.getProcessor.TaskListener() {
            @Override
            public void onFinished(ArrayList<String> result, Context context) {

            }

            @Override
            public void onComplete(ArrayList<ArrayList<String>> result, Context context) {
                if (result != null) {
                    yelp_returns = result;
                    mapReady = true;
                    googleClientServices();
                    renderMap();
                }

            }
        });

        getProcessor.execute("https://api.yelp.com/v3/businesses/search?text=restaurants&latitude=" + latitude + "&longitude=" + longitude, "1", access_token);
    }

    public GoogleMap addYelpBusinessMarkers(ArrayList<ArrayList<String>> result, GoogleMap map) {
        if (result != null) {
            for (int i = 0; i < result.size(); i++) {
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.title(result.get(i).get(0));
                markerOptions.position(new LatLng(Double.parseDouble(result.get(i).get(5)), Double.parseDouble(result.get(i).get(6))));
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
                map.addMarker(markerOptions);
            }
        }
        return map;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case LOCATION_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    createLocationRequest();
                    gapiClient = new GoogleApiClient.Builder(this)
                            .addApi(LocationServices.API)
                            .addConnectionCallbacks(this)
                            .addOnConnectionFailedListener(this)
                            .build();
                    mapFragment = MapFragment.newInstance();
                    FragmentTransaction fragmentTransaction =
                            getFragmentManager().beginTransaction();
                    fragmentTransaction.add(R.id.map, mapFragment);
                    fragmentTransaction.commit();

                } else {
                    AlertDialog.Builder builder;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
                    } else {
                        builder = new AlertDialog.Builder(this);
                    }

                    builder.setTitle("Permission required").setMessage("App cannot continue, enable permissions and restart app");
                    builder.setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //restart application
                            ProcessPhoenix.triggerRebirth(getApplicationContext());
                        }
                    });
                    builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //shutdown application
                            Intent intent = new Intent(getApplicationContext(), Home.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intent.putExtra("EXIT", true);
                            startActivity(intent);
                        }
                    });
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();

                }

            }


        }
    }

    protected void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(8000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    public void getLocationUpdates() {
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(gapiClient, locationRequest, this);
            lastLocation = LocationServices.FusedLocationApi.getLastLocation(gapiClient);
        } catch (SecurityException e) {
            System.out.println(e);
        }

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        try {
            //Toast.makeText(this, "Connected to Google Api Client", Toast.LENGTH_LONG).show();
            getLocationUpdates();
            if (lastLocation != null) {
                latitude = lastLocation.getLatitude();
                longitude = lastLocation.getLongitude();
                MapFragment mapFragment;
                mapFragment = (MapFragment) getFragmentManager()
                        .findFragmentById(R.id.map);
                mapFragment.getMapAsync(this);
            } else {
                getLocationUpdates();
            }
        } catch (SecurityException e) {
            System.out.println(e);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        //Toast.makeText(getApplicationContext(), "suspended", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        //Toast.makeText(getApplicationContext(), "Connection Failure, retry", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        //Toast.makeText(getApplicationContext(), "User Moved", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        MarkerOptions markerOptions = new MarkerOptions();
        map.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        markerOptions.position(new LatLng(latitude, longitude));
        markerOptions.title("You");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        if (mapReady) {
            map = addYelpBusinessMarkers(yelp_returns, map);
            System.out.println("INFO" + " marker clicked");
            map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    final Dialog dialog = new Dialog(Home.this);
                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    int width = (int)(getResources().getDisplayMetrics().widthPixels);
                    int height = (int)(getResources().getDisplayMetrics().heightPixels*0.65);
                    if(dialog.getWindow()!=null)
                    {
                        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
                        dialog.setCanceledOnTouchOutside(true);
                    }
                    else
                    {
                        System.out.println("dialog null");
                    }
                    dialog.setContentView(R.layout.infowindowlayout);
                    dialog.getWindow().setLayout(width, height);
                    imageView = (ImageView) dialog.findViewById(R.id.imageV);
                    imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                    TextView name = (TextView) dialog.findViewById(R.id.Name);
                    TextView address = (TextView) dialog.findViewById(R.id.address);
                    TextView phone = (TextView) dialog.findViewById(R.id.phone);
                    TextView price = (TextView) dialog.findViewById(R.id.price);
                    RatingBar ratingBar = (RatingBar)dialog.findViewById(R.id.ratingBar);
                    ratingBar.setActivated(false);
                    ratingBar.setBackgroundColor(Color.TRANSPARENT);
                    int index = -1;
                    for (int i = 0; i < yelp_returns.size(); i++) {
                        String lat = "" + marker.getPosition().latitude;
                        String lon = "" + marker.getPosition().longitude;
                        if (yelp_returns.get(i).get(5).equals(lat) && yelp_returns.get(i).get(6).equals(lon)) {
                            index = i;
                            break;
                        }
                    }
                    try {
                        if (index != -1) {
                            URL url = new URL(yelp_returns.get(index).get(7));
                            System.out.println(url.toString());
                            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                            StrictMode.setThreadPolicy(policy);
                            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                            connection.setDoInput(true);
                            connection.connect();
                            BufferedInputStream bufferedInputStream = new BufferedInputStream(connection.getInputStream(), 1024);
                            Bitmap bmp = BitmapFactory.decodeStream(bufferedInputStream);
                            if (imageView != null && bmp != null) {
                                imageView.setImageBitmap(bmp);
                            }
                            bufferedInputStream.close();
                            name.setText("  " + yelp_returns.get(index).get(0));
                            ratingBar.setRating(Float.parseFloat(yelp_returns.get(index).get(1)));
                            address.setText("  " + yelp_returns.get(index).get(2));
                            phone.setText("  " + yelp_returns.get(index).get(3));
                            price.setText("  " + yelp_returns.get(index).get(4));
                        } else {
                            name.setText("You are at");
                            address.setText("" + latitude + ", " + longitude);
                            phone.setText(" ");
                            price.setText(" ");
                        }

                    } catch (MalformedURLException e) {

                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    dialog.show();
                    return true;
                }
            });

            marker = map.addMarker(markerOptions);
            CameraPosition cameraPosition = new CameraPosition.Builder().target(new LatLng(latitude, longitude)).zoom(15).tilt(30).build();
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 15));
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 2000, null);
        } else {
            marker = map.addMarker(markerOptions);
            CameraPosition cameraPosition = new CameraPosition.Builder().target(new LatLng(latitude, longitude)).zoom(15).tilt(30).build();
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 15));
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 2000, null);
        }


    }

}
