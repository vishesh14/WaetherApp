package com.waetherapp.app;

import static com.waetherapp.app.location.CityFinder.getCityNameUsingNetwork;
import static com.waetherapp.app.location.CityFinder.setLongitudeLatitude;
import static com.waetherapp.app.network.InternetConnectivity.isInternetConnected;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.waetherapp.app.adapter.DaysAdapter;
import com.waetherapp.app.databinding.ActivityHomeBinding;
import com.waetherapp.app.location.LocationCall;
import com.waetherapp.app.update.UpdateUI;
import com.waetherapp.app.url.URL;

import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends ComponentActivity {
    private final int WEATHER_FORECAST_APP_UPDATE_REQ_CODE = 101;   // for app update
    private static final int PERMISSION_CODE = 1;                   // for user location permission
    private String name, updated_at, pressure, wind_speed, humidity;
    private int condition;
    private long update_time;
    private String city = "";
    private final int REQUEST_CODE_EXTRA_INPUT = 101;
    private ActivityHomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // binding
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        // when user do search and refresh
        listeners();

        // getting data using internet connection
        getDataUsingNetwork();

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_EXTRA_INPUT){
            if(resultCode == RESULT_OK && data!=null){
                ArrayList<String> arrayList = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                binding.layout.cityEt.setText(Objects.requireNonNull(arrayList).get(0).toUpperCase());
                searchCity(binding.layout.cityEt.getText().toString());
            }
        }
    }


    private void setUpDaysRecyclerView() {
        DaysAdapter daysAdapter = new DaysAdapter(this);
        binding.dayRv.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );
        binding.dayRv.setAdapter(daysAdapter);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void listeners() {
        binding.layout.main.setOnTouchListener((view, motionEvent) -> {
            hideKeyboard(view);
            return false;
        });
        binding.layout.searchBarIv.setOnClickListener(view -> searchCity(binding.layout.cityEt.getText().toString()));
        binding.layout.searchBarIv.setOnTouchListener((view, motionEvent) -> {
            hideKeyboard(view);
            return false;
        });
        binding.layout.cityEt.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_GO) {
                searchCity(binding.layout.cityEt.getText().toString());
                hideKeyboard(textView);
                return true;
            }
            return false;
        });
        binding.layout.cityEt.setOnFocusChangeListener((view, b) -> {
            if (!b) {
                hideKeyboard(view);
            }
        });

    }


    private void searchCity(String cityName) {
        if (cityName == null || cityName.isEmpty()) {
            Toast.makeText(this,"Please enter the city name", Toast.LENGTH_LONG).show();
        } else {
            setLatitudeLongitudeUsingCity(cityName);
        }
    }

    private void getDataUsingNetwork() {
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);
        //check permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_CODE);
        } else {
            client.getLastLocation().addOnSuccessListener(location -> {
                setLongitudeLatitude(location);
                city = getCityNameUsingNetwork(this, location);
                getTodayWeatherInfo(city);
            });
        }
    }

    private void setLatitudeLongitudeUsingCity(String cityName) {
        URL.setCity_url(cityName);
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, URL.getCity_url(), null, response -> {
            try {
                LocationCall.lat = response.getJSONObject("coord").getString("lat");
                LocationCall.lon = response.getJSONObject("coord").getString("lon");
                getTodayWeatherInfo(cityName);
                // After the successfully city search the cityEt(editText) is Empty.
                binding.layout.cityEt.setText("");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }, error ->  Toast.makeText(this,"Please enter the city name", Toast.LENGTH_LONG).show());
        requestQueue.add(jsonObjectRequest);
    }

    @SuppressLint("DefaultLocale")
    private void getTodayWeatherInfo(String name) {
        URL url = new URL();
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url.getLink(), null, response -> {
            try {
                this.name = name;
                update_time = response.getJSONObject("current").getLong("dt");
                updated_at = new SimpleDateFormat("EEEE hh:mm a", Locale.ENGLISH).format(new Date(update_time * 1000));
                condition = response.getJSONArray("daily").getJSONObject(0).getJSONArray("weather").getJSONObject(0).getInt("id");
                pressure = response.getJSONArray("daily").getJSONObject(0).getString("pressure");
                wind_speed = response.getJSONArray("daily").getJSONObject(0).getString("wind_speed");
                humidity = response.getJSONArray("daily").getJSONObject(0).getString("humidity");

                updateUI();
                hideProgressBar();
                setUpDaysRecyclerView();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }, null);
        requestQueue.add(jsonObjectRequest);
        Log.i("json_req", "Day 0");
    }

    @SuppressLint("SetTextI18n")
    private void updateUI() {
        binding.layout.nameTv.setText(name);
        updated_at = translate(updated_at);
//        binding.layout.updatedAtTv.setText(updated_at);
//        binding.layout.conditionIv.setImageResource(
//                getResources().getIdentifier(
//                        UpdateUI.getIconID(condition, update_time, sunrise, sunset),
//                        "drawable",
//                        getPackageName()
//                ));
//        binding.layout.conditionDescTv.setText(description);
//        binding.layout.tempTv.setText(temperature + "°C");
//        binding.layout.minTempTv.setText(min_temperature + "°C");
//        binding.layout.maxTempTv.setText(max_temperature + "°C");
        binding.layout.pressureTv.setText(pressure + " mb");
        binding.layout.windTv.setText(wind_speed + " km/h");
        binding.layout.humidityTv.setText(humidity + "%");
    }

    private String translate(String dayToTranslate) {
        String[] dayToTranslateSplit = dayToTranslate.split(" ");
        dayToTranslateSplit[0] = UpdateUI.TranslateDay(dayToTranslateSplit[0].trim(), getApplicationContext());
        return dayToTranslateSplit[0].concat(" " + dayToTranslateSplit[1]);
    }

    private void hideProgressBar() {
        //binding.progress.setVisibility(View.GONE);
        binding.layout.main.setVisibility(View.VISIBLE);
    }

    private void hideMainLayout() {
       // binding.progress.setVisibility(View.VISIBLE);
        binding.layout.main.setVisibility(View.GONE);
    }

    private void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) view.getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void checkConnection() {
        if (!isInternetConnected(this)) {
            hideMainLayout();
            Toast.makeText(this,"Please check your internet connection", Toast.LENGTH_LONG).show();
        } else {
            hideProgressBar();
            getDataUsingNetwork();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,"Permission Granted", Toast.LENGTH_LONG).show();
                getDataUsingNetwork();
            } else {
                Toast.makeText(this,"Permission Denied", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkConnection();
    }

}
