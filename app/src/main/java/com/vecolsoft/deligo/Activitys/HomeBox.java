package com.vecolsoft.deligo.Activitys;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.vecolsoft.deligo.Common.Common;
import com.vecolsoft.deligo.Modelo.FCMResponse;
import com.vecolsoft.deligo.Modelo.Notification;
import com.vecolsoft.deligo.Modelo.Rider;
import com.vecolsoft.deligo.Modelo.Sender;
import com.vecolsoft.deligo.Modelo.Token;
import com.vecolsoft.deligo.R;
import com.vecolsoft.deligo.Remote.GetGson;
import com.vecolsoft.deligo.Remote.IFCMService;
import com.vecolsoft.deligo.Utils.InternetConnection;
import com.vecolsoft.deligo.Utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class HomeBox extends AppCompatActivity implements
        OnMapReadyCallback,
        LocationEngineListener,
        PermissionsListener {

    private SharedPreferences prefs;

    // variables for adding location layer
    private MapboxMap mapboxMap;
    private PermissionsManager permissionsManager;
    private LocationEngine locationEngine;
    private Location originLocation;
    private LocationManager locationManager;

    // variable for adding map
    private MapView mapView;
    private Marker mUserMarker;

    //elementos del toolbar
    private Toolbar toolbar;
    private CircleImageView circleImageView;
    private RelativeLayout perfil;
    /////////////////////////////////

    //ELEMENTOS
    private AppCompatButton btnSolicitarDeli;
    private TextView txtLocation;

    boolean isDriverFound = false;
    String DriverId = "";
    int Radius = 1; //Radio de 1 Kilometro(s)
    int Distancia = 1; //
    private static final int Limite = 3;

    //enviar alerta
    IFCMService mService;

    //Contexto
    private Context c = this;

    //verificar internet
    boolean connected = false;

    //Sistema de precencia
    DatabaseReference DriversAvailable;

    private static final int INTERVALO = 2000; //2 segundos para salir
    private long tiempoPrimerClick;

    //////////Elementos de JsonParsing
    GetGson mGetGson;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Bolquear rotacion
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));

        //      Fuente
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/Roboto-Regular.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build());

        setContentView(R.layout.activity_home_box);

        mService = Common.getFCMService();
        mGetGson = Common.getGson();

        prefs = getSharedPreferences("datos", Context.MODE_PRIVATE);



        mapView = (MapView) findViewById(R.id.mapViewBox);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        //perfil
        perfil = (RelativeLayout) findViewById(R.id.perfil);
        perfil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startActivity(new Intent(HomeBox.this, PerfilActivity.class));
                //overridePendingTransition( R.anim.slide_in_up, R.anim.slide_out_up );
            }
        });

        //Toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //location TextView
        txtLocation = (TextView) findViewById(R.id.txtLocation);


        //BotonSolicitar
        btnSolicitarDeli = (AppCompatButton) findViewById(R.id.btnSolicitarDeli);
        btnSolicitarDeli.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!isDriverFound) {
                    SolicitarDeli(FirebaseAuth.getInstance().getCurrentUser().getUid());
                } else {
                    SendRequesToDriver(DriverId);
                }
            }
        });

        CheckGPSStatus();
        updateFirebaseToken();




        //verificar internet
        if (InternetConnection.checkConnection(c)) {
            // Its Available...

            //verificar si hay internet con ping
            if (InternetConnection.internetIsConnected(c)) {
                enableLocationPlugin();
                connected = true;


            } else {

                AlertDialog.Builder dialogo = new AlertDialog.Builder(this);
                dialogo.setTitle("Error de coneccion.");
                dialogo.setMessage("Fue imposible establecer una coneccion a internet");
                dialogo.setCancelable(false);
                dialogo.setIcon(R.drawable.ic_error);

                dialogo.setPositiveButton("Reintentar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                        startActivity(getIntent());
                    }
                });

                dialogo.show();

            }

        } else {
            // Not Available...
            connected = false;

            AlertDialog.Builder dialogo = new AlertDialog.Builder(this);
            dialogo.setTitle("Error de coneccion.");
            dialogo.setMessage("Fue imposible establecer una coneccion a internet");
            dialogo.setCancelable(false);
            dialogo.setIcon(R.drawable.ic_error);

            dialogo.setPositiveButton("Reintentar", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                    startActivity(getIntent());
                }
            });

            dialogo.show();

        }
    }

    private void updateFirebaseToken() {

        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference tokens = db.getReference(Common.token_tbl);

        Token token = new Token(FirebaseInstanceId.getInstance().getToken());
        tokens.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .setValue(token);

    }

    private void SendRequesToDriver(String driverId) {
        DatabaseReference tokens = FirebaseDatabase.getInstance().getReference(Common.token_tbl);

        tokens.orderByKey().equalTo(driverId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot postSnapShot : dataSnapshot.getChildren()) {
                            Token token = postSnapShot.getValue(Token.class);

                            String json_lat_lng = new Gson().toJson(new LatLng(originLocation.getLatitude(), originLocation.getLongitude()));
                            String riderToken = FirebaseInstanceId.getInstance().getToken();
                            Notification data = new Notification(riderToken, json_lat_lng); //enviar esto a la app driver
                            Sender content = new Sender(token.getToken(), data); // enviar esta data al token

                            mService.sendMessage(content)
                                    .enqueue(new Callback<FCMResponse>() {
                                        @Override
                                        public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                                            if (response.body().success == 1) {
                                                Toast.makeText(HomeBox.this, "Solicitud enviada.", Toast.LENGTH_SHORT).show();
                                            } else {
                                                Toast.makeText(HomeBox.this, "Error de solicitud.", Toast.LENGTH_SHORT).show();
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<FCMResponse> call, Throwable t) {
                                            Log.d("ERROR", t.getMessage());
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    private void SolicitarDeli(String uid) {

        final double latitude = originLocation.getLatitude();
        final double longitude = originLocation.getLongitude();


        DatabaseReference dbRequest = FirebaseDatabase.getInstance().getReference(Common.pickup_request_tbl);
        GeoFire mGeoFire = new GeoFire(dbRequest);
        mGeoFire.setLocation(uid, new GeoLocation(latitude, longitude),
                new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                    }
                });


        //if (mUserMarker.isVisible()) {
        mUserMarker.remove();
        mUserMarker = mapboxMap.addMarker(new MarkerOptions()
                .position(new LatLng(latitude, longitude))
                .title("Recojer aqui"));

        //mUserMarker.showInfoWindow();
        mapboxMap.selectMarker(mUserMarker);

        btnSolicitarDeli.setText("obteniendo Deli");
        //}

        BuscarConductor();

    }

    private void BuscarConductor() {
        DatabaseReference drivers = FirebaseDatabase.getInstance().getReference(Common.driver_tbl);
        GeoFire gfDrivers = new GeoFire(drivers);

        GeoQuery geoQuery = gfDrivers.queryAtLocation(new GeoLocation(originLocation.getLatitude(), originLocation.getLongitude()), Radius);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                // Si encuentra

                if (!isDriverFound) {

                    isDriverFound = true;
                    DriverId = key;
                    btnSolicitarDeli.setText("llamar deli");
                    Toast.makeText(HomeBox.this, "Encontrado", Toast.LENGTH_SHORT).show();

                    //animar camara a 2 puntos
                    //setCameraPosition();

                }


            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {

                //Si no encuentra conductores incrementa la distancia

                if (!isDriverFound) {

                    Radius++;
                    BuscarConductor();
                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

    }

    private void LoadAllAvailableDrivers() {

        mapboxMap.clear();
        mapboxMap.addMarker(new MarkerOptions().position(new LatLng(originLocation.getLatitude(), originLocation.getLongitude()))
                .title("Tu"));

        // leer todos los conducteres avilitados en un radio de 3 km.

        DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference(Common.driver_tbl);
        GeoFire gf = new GeoFire(driverLocation);

        GeoQuery geoQuery = gf.queryAtLocation(new GeoLocation(originLocation.getLatitude(), originLocation.getLongitude()), Distancia);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, final GeoLocation location) {

                FirebaseDatabase.getInstance().getReference(Common.user_driver_tbl)
                        .child(key)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {

                                Rider rider = dataSnapshot.getValue(Rider.class);

                                // Create an Icon object for the marker to use
                                IconFactory iconFactory = IconFactory.getInstance(HomeBox.this);
                                Icon icon = iconFactory.fromResource(R.drawable.circlemo);

                                //Add driver to map
                                mapboxMap.addMarker(new MarkerOptions()
                                        .position(new LatLng(location.latitude, location.longitude))
                                        .title(rider.getName())
                                        .snippet(rider.getPhone())
                                        .icon(icon));

                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });

            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {

                if (Distancia <= Limite) { // distance  jus  find  for 3 km
                    Distancia++;
                    LoadAllAvailableDrivers();

                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        mapboxMap.getUiSettings().setAttributionEnabled(false);
        mapboxMap.getUiSettings().setLogoEnabled(false);
        mapboxMap.getUiSettings().setTiltGesturesEnabled(false);
        mapboxMap.getUiSettings().setRotateGesturesEnabled(false);
        displayLocation();
    }

    private void displayLocation() {

        if (originLocation != null) {

            //Sistema de precencia
            DriversAvailable = FirebaseDatabase.getInstance().getReference(Common.driver_tbl);
            DriversAvailable.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    LoadAllAvailableDrivers();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

            //añadir marcador marker
            if (mUserMarker != null) {
                mUserMarker.remove();
            }
            mUserMarker = mapboxMap.addMarker(new MarkerOptions()
                    .position(new LatLng(originLocation.getLatitude(), originLocation.getLongitude()))
                    .title("fddosfon")
                    .snippet("foehfeof"));

            CameraPosition position = new CameraPosition.Builder()
                    .target(new LatLng(originLocation.getLatitude(), originLocation.getLongitude())) // Sets the new camera position
                    .zoom(15) // Sets the zoom to level 10
                    .tilt(20) // Set the camera tilt to 20 degrees
                    .build(); // Builds the CameraPosition object from the builder

            mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position));

            LoadAllAvailableDrivers();
        } else {
            Log.d("ERROR", "No se puede obtener la localisacion.");
        }


    }

    @SuppressWarnings({"MissingPermission"})
    private void enableLocationPlugin() {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            initializeLocationEngine();
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    private void stopLocationEngine() {

        if (locationEngine != null) {
            locationEngine.deactivate();
            mUserMarker.remove();
        }
    }

    @SuppressWarnings({"MissingPermission"})
    private void initializeLocationEngine() {
        LocationEngineProvider locationEngineProvider = new LocationEngineProvider(this);
        locationEngine = locationEngineProvider.obtainBestLocationEngineAvailable();
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.activate();

        Location lastLocation = locationEngine.getLastLocation();
        if (lastLocation != null) {
            originLocation = lastLocation;
        } else {
            locationEngine.addLocationEngineListener(this);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.Opciones:
                Toast.makeText(this, "Opciones!", Toast.LENGTH_SHORT).show();
                break;
            case R.id.Salir:
                AlertDialog.Builder dialogo = new AlertDialog.Builder(this);
                dialogo.setTitle("¿Cerrar secion?");
                dialogo.setCancelable(false);
                dialogo.setIcon(R.drawable.ic_exit);

                dialogo.setPositiveButton("Cerrar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Utils.removesharedpreferencies(prefs);
                        logout();
                    }
                });
                dialogo.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                dialogo.show();
                break;

        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        //Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocationPlugin();
        } else {
            //Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @SuppressWarnings({"MissingPermission"})
    @Override
    protected void onStart() {
        super.onStart();

        if (locationEngine != null) {
            locationEngine.requestLocationUpdates();
        }
        mapView.onStart();

    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (locationEngine != null) {
            locationEngine.removeLocationUpdates();
        }
        mapView.onStop();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationEngine != null) {
            locationEngine.deactivate();
        }
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    @SuppressWarnings("MissingPermission")
    public void onConnected() {
        locationEngine.requestLocationUpdates();
        if (originLocation != null) {
            displayLocation();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            originLocation = location;
            displayLocation();
            getLocation();
        }
    }


    //////////////////////////// MIOS

    private void getLocation() {

        String requestApi = null;
        try {

            requestApi = "https://api.mapbox.com/geocoding/v5/mapbox.places/" +
                    originLocation.getLongitude() + "," + originLocation.getLatitude() +
                    ".json?" +
                    "access_token=" + getResources().getString(R.string.access_token);

            Log.d("vencolsoft", requestApi); //print url for debug

            mGetGson.getPath(requestApi)
                    .enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {

                            try {
                                JSONObject jsonObject = new JSONObject(response.body().toString());

                                JSONArray features = jsonObject.getJSONArray("features");

                                JSONObject object = features.getJSONObject(0);

                                JSONObject properties = object.getJSONObject("properties");

//                                Get Address
                                String address = properties.getString("address");
                                txtLocation.setText(address);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {
                            Toast.makeText(c, "" + t.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void logout() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    public void CheckGPSStatus() {

        //verificar si el gps esta activo
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            AlertNoGps();
        }
    }

    private void AlertNoGps() {

        AlertDialog.Builder dialogo = new AlertDialog.Builder(this);
        dialogo.setTitle("El sistema GPS esta desactivado, ¿Desea activarlo?");
        dialogo.setCancelable(false);
        dialogo.setIcon(R.drawable.ic_location_disabled);

        dialogo.setPositiveButton("SI", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        });
        dialogo.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        dialogo.show();
    }

    @Override
    public void onBackPressed() {
        if (tiempoPrimerClick + INTERVALO > System.currentTimeMillis()) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return;
        } else {
            Toast.makeText(this, "Vuelve a presionar para salir", Toast.LENGTH_SHORT).show();
        }
        tiempoPrimerClick = System.currentTimeMillis();
    }

}