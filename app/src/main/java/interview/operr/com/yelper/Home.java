package interview.operr.com.yelper;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.vision.text.Text;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class Home extends FragmentActivity implements OnMapReadyCallback,GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
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
    GoogleMap googleMap;
    Boolean mapReady = false;
    ImageView imageView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        mapReady = false;
        sharedPreferences = getSharedPreferences("Yelper", Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        imageView = (ImageView) findViewById(R.id.imageV);
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION);
        if(permissionCheck!= PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_REQUEST);
        }
        else
        {
            //http://www.mocky.io/v2/591e09733f00009b0b77c930 -- MOCK REST endpoint to access clientID and clientSecret
            Utils.getProcessor getProcessor = new Utils.getProcessor(getApplicationContext(), new Utils.getProcessor.TaskListener(){

                @Override
                public void onFinished(ArrayList<String> result, Context context) {
                    credentials = result;
                    String clientID = credentials.get(0);
                    String clientSecret = credentials.get(1);
                    editor.putString("clientID", clientID);
                    editor.putString("clientSecret",clientSecret);
                    editor.commit();
                    googleClientServices();
                    getYelpAccessToken();
                }

                @Override
                public void onComplete(ArrayList<ArrayList<String>> result, Context context) {

                }
            });
            getProcessor.execute("http://www.mocky.io/v2/591e09733f00009b0b77c930","0");

        }
    }

    public void renderMap()
    {
        mapFragment = MapFragment.newInstance();
        FragmentTransaction fragmentTransaction =
                getFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.map, mapFragment);
        fragmentTransaction.commit();

    }

    public void googleClientServices()
    {
        createLocationRequest();
        gapiClient=new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        gapiClient.connect();
    }
    public void getYelpAccessToken()
    {
        Utils.postProcessor postProcessor = new Utils.postProcessor(getApplicationContext(), new Utils.postProcessor.TaskListener() {
            @Override
            public void onFinished(ArrayList<String> s, Context context) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("access_token",s.get(0));
                editor.commit();
                getRestaurants(s.get(0));


            }
        });

        postProcessor.execute(sharedPreferences.getString("clientID",""),sharedPreferences.getString("clientSecret",""),"https://api.yelp.com/oauth2/token");
    }

    public void getRestaurants(String access_token)
    {
        Utils.getProcessor getProcessor = new Utils.getProcessor(getApplicationContext(), new Utils.getProcessor.TaskListener() {
            @Override
            public void onFinished(ArrayList<String> result, Context context) {

            }

            @Override
            public void onComplete(ArrayList<ArrayList<String>> result, Context context) {
                yelp_returns = result;
                mapReady = true;
                googleClientServices();
                renderMap();
            }
        });

        getProcessor.execute("https://api.yelp.com/v3/businesses/search?text=restaurants&latitude="+latitude+"&longitude="+longitude,"1",access_token);
    }

    public GoogleMap addYelpBusinessMarkers(ArrayList<ArrayList<String>> result, GoogleMap map)
    {

        for(int i = 0 ; i < result.size(); i++)
        {
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.title(result.get(i).get(0));
            markerOptions.position(new LatLng(Double.parseDouble(result.get(i).get(5)), Double.parseDouble(result.get(i).get(6))));
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
            map.addMarker(markerOptions);
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
                    gapiClient=new GoogleApiClient.Builder(this)
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
                    builder = new AlertDialog.Builder(this);

                    builder.setTitle("Permission denied").setMessage("App cannot continue, enable permission ?");
                    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                }
                return;
            }


        }
    }

    protected void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(8000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    public void getLocationUpdates()
    {
        try
        {
            LocationServices.FusedLocationApi.requestLocationUpdates(gapiClient, locationRequest,this);
            lastLocation = LocationServices.FusedLocationApi.getLastLocation(gapiClient);
        }
        catch (SecurityException e)
        {
            System.out.println(e);
        }

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        try
        {
            //Toast.makeText(this, "Connected to Google Api Client", Toast.LENGTH_LONG).show();
            getLocationUpdates();
            if (lastLocation != null) {
                latitude=lastLocation.getLatitude();
                longitude=lastLocation.getLongitude();
                MapFragment mapFragment;
                mapFragment = (MapFragment) getFragmentManager()
                        .findFragmentById(R.id.map);
                mapFragment.getMapAsync(this);
            }
            else
            {
                getLocationUpdates();
            }
        }
        catch(SecurityException e)
        {
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
        if(mapReady)
        {
            map = addYelpBusinessMarkers(yelp_returns, map);
            /*map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
                @Override
                public View getInfoWindow(Marker marker) {
                    return null;
                }

                @Override
                public View getInfoContents(Marker marker) {
                    View view = getLayoutInflater().inflate(R.layout.infowindowlayout, null);
                    return null;
                }
            });*/
            map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    final Dialog dialog = new Dialog(Home.this);
                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
                    dialog.setContentView(R.layout.infowindowlayout);
                    imageView = (ImageView) dialog.findViewById(R.id.imageV);
                    TextView name = (TextView) dialog.findViewById(R.id.Name);
                    TextView address = (TextView) dialog.findViewById(R.id.address);
                    TextView phone = (TextView) dialog.findViewById(R.id.phone);
                    TextView rating = (TextView) dialog.findViewById(R.id.rating);
                    TextView price = (TextView) dialog.findViewById(R.id.price);
                    int index = -1;
                    for(int i = 0; i < yelp_returns.size(); i++)
                    {
                        String lat = ""+marker.getPosition().latitude;
                        String lon = ""+marker.getPosition().longitude;
                        if(yelp_returns.get(i).get(5).equals(lat) && yelp_returns.get(i).get(6).equals(lon))
                        {
                            index = i;
                            break;
                        }
                    }
                    try
                    {
                        if(index!=-1)
                        {
                            URL url = new URL(yelp_returns.get(index).get(7));
                            System.out.println(url.toString());
                            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                            StrictMode.setThreadPolicy(policy);
                            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                            connection.setDoInput(true);
                            connection.connect();
                            BufferedInputStream bufferedInputStream = new BufferedInputStream(connection.getInputStream(), 1024);
                            Bitmap bmp = BitmapFactory.decodeStream(bufferedInputStream);
                            if(imageView!=null && bmp!=null)
                            {
                                imageView.setImageBitmap(bmp);
                            }
                            bufferedInputStream.close();
                            name.setText(""+yelp_returns.get(index).get(0));
                            rating.setText("Rating: "+yelp_returns.get(index).get(1));
                            address.setText("Address: "+yelp_returns.get(index).get(2));
                            phone.setText("Phone: "+yelp_returns.get(index).get(3));
                            price.setText("Price: "+yelp_returns.get(index).get(4));
                        }
                        else
                        {
                            name.setText("You are at");
                            rating.setText(""+latitude+", "+longitude);
                            address.setText(" ");
                            phone.setText(" ");
                            price.setText(" ");
                        }

                    }
                    catch(MalformedURLException e)
                    {

                        e.printStackTrace();
                    }
                    catch(IOException e)
                    {
                        e.printStackTrace();
                    }
                    Button button = (Button) dialog.findViewById(R.id.close);
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.cancel();
                        }
                    });
                    dialog.show();
                    return true;
                }
            });

            marker = map.addMarker(markerOptions);
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 15));
            map.animateCamera(CameraUpdateFactory.zoomTo(15),  2000, null);
        }
        else
        {
            marker = map.addMarker(markerOptions);
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 15));
            map.animateCamera(CameraUpdateFactory.zoomTo(15),  2000, null);
        }


    }

}
