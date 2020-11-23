package jm.vega.tiempineitor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import android.os.Bundle;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

import jm.vega.tiempineitor.R;


public class MainActivity extends AppCompatActivity implements LocationListener {

    private final String[] permissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.INTERNET
    };
    private LocationManager locationManager;
    private Location lastLocation;
    private RequestQueue requestQueue;
    private TextView placeTv;
    private TextView tempTv;
    private TextView forecastTv;
    private ImageView icon;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        placeTv = findViewById(R.id.textPlace);
        tempTv = findViewById(R.id.textTemp);
        forecastTv=findViewById(R.id.textForecast);
        icon = findViewById(R.id.currentIcon);


        checkPermissions();
        initializeRequestQueue();
        initializeLocation();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, R.string.can_not_continue, Toast.LENGTH_LONG).show();
                finish();
                break;
            }
        }
    }

    private void checkPermissions() {

        for (String permission : permissions) {
            if (checkSelfPermission(permission) != (PackageManager.PERMISSION_GRANTED)) {
                requestPermissions(permissions, 1);
                return;
            }
        }
    }
    private void initializeRequestQueue(){
        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // 1MB cap
        Network network = new BasicNetwork(new HurlStack());
        requestQueue = new RequestQueue(cache, network);
        requestQueue.start();
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        if(location.distanceTo(lastLocation)>=1000){
            updateWeatherInfo(location);
        }
    }

    @SuppressLint("MissingPermission")
    private void initializeLocation() {
        lastLocation = new Location("dummyProvider");
        lastLocation.setLatitude(0);
        lastLocation.setAltitude(0);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000*60*5, 1000, this);
    }

    private void updateWeatherInfo(Location location){

        String url = String.format(BuildConfig.API_URL+"?key=%1$s&q=%2$s&lang=%3$s",
                BuildConfig.API_KEY,
                location.getLatitude()+","+location.getLongitude(),
                Locale.getDefault().getLanguage() );
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
        new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                lastLocation=location;
                paintData(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                onCallFailure();
            }
        });

        requestQueue.add(jsonObjectRequest);
    }

    private void onCallFailure(){
        Toast.makeText(this, R.string.error_on_call, Toast.LENGTH_LONG).show();

    }

    private void paintData(JSONObject data){
        try {
            placeTv.setText(
                    String.format(
                            getString(R.string.placeIntro),
                            data.getJSONObject("location").getString("name"))
            );

            tempTv.setText( String.format(
                    getString(R.string.tempIntro),
                    data.getJSONObject("current").getString("temp_c")));

            forecastTv.setText(data.getJSONObject("current").getJSONObject("condition").getString("text"));
            requestImage("https:"+data.getJSONObject("current").getJSONObject("condition").getString("icon").replace("64x64","128x128"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
    private void requestImage(String uri){
        ImageRequest imageRequest = new ImageRequest(uri, new Response.Listener<Bitmap>() {
            @Override
            public void onResponse(Bitmap response) {
                paintImage(response);

            }
        }, 128, 128, ImageView.ScaleType.MATRIX, Bitmap.Config.RGB_565, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                onCallFailure();
            }
        });
        requestQueue.add(imageRequest);
    }
    private void paintImage(Bitmap image){
        icon.setImageBitmap(image);
    }

}