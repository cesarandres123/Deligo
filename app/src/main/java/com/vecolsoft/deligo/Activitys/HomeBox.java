package com.vecolsoft.deligo.Activitys;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
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
import com.vecolsoft.deligo.Modelo.DataMessage;
import com.vecolsoft.deligo.Modelo.FCMResponse;
import com.vecolsoft.deligo.Modelo.Notification;
import com.vecolsoft.deligo.Modelo.Rider;
import com.vecolsoft.deligo.Modelo.Sender;
import com.vecolsoft.deligo.Modelo.Token;
import com.vecolsoft.deligo.R;
import com.vecolsoft.deligo.Remote.IFCMService;
import com.vecolsoft.deligo.Utils.InternetConnection;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

    // variables for adding location layer
    private MapboxMap mapboxMap;
    private PermissionsManager permissionsManager;
    private LocationEngine locationEngine;
    private LocationManager locationManager;

    //geocider location
    Geocoder geocoder;

    private static int UPDATE_INTERVAL = 5000;
    private static int FASTEST_INTERVAL = 3000;
    private static int DISPLACEMENT = 10;

    // variable for adding map
    private MapView mapView;
    private Marker mUserMarker;


    //elementos del toolbar
    private Toolbar toolbar;
    private CircleImageView circleImageView;
    private RelativeLayout perfil;
    private TextView nombre;
    /////////////////////////////////

    //ELEMENTOS
    private static AppCompatButton btnSolicitarDeli;
    private TextView txtLocation;
    private static ProgressBar progressBar;

    //elementos del cardview del conductor
    static CardView cdw_conductor;
    private CircleImageView img_imagen_conductor;
    private TextView txv_nombre_conductor;
    private TextView txv_telefono_conductor;
    private TextView txv_rango_conductor;
    private TextView txv_realisadas_conductor;
    private ImageButton imb_cancelar_conductor;

    //elementos del cardview del conductor2
    static CardView cdw_conductor2;
    private CircleImageView img_imagen_conductor2;
    private TextView txv_nombre_conductor2;
    private TextView txv_info_conductor2;
    private ImageButton imb_cancelar_conductor2;


    String DriverId = "";
    int Radius = 1; //Radio de 1 Kilometro(s)
    int Distancia = 1; //
    private static final int Limite = 3;

    private double DriverLat;
    private double DriverLng;

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

    // eleiminar pickuprequest database
    private static DatabaseReference mdatabaseRemove;


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

        mapView = (MapView) findViewById(R.id.mapViewBox);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        //perfil
        perfil = (RelativeLayout) findViewById(R.id.perfil);
        perfil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startActivity(new Intent(HomeBox.this, PerfilActivity.class));
            }
        });

        //Toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        nombre = (TextView) findViewById(R.id.tvNombre);
        nombre.setText(Common.CurrentUser.getName());
        circleImageView = (CircleImageView) findViewById(R.id.profile_image);

        //load Avatar
        if (Common.CurrentUser.getAvatarUrl() != null && !TextUtils.isEmpty(Common.CurrentUser.getAvatarUrl())) {

            Glide.with(this)
                    .load(Common.CurrentUser.getAvatarUrl())
                    .into(circleImageView);
        }

        //location TextView
        txtLocation = (TextView) findViewById(R.id.txtLocation);

        //progressbar
        progressBar = (ProgressBar) findViewById(R.id.progressBar2);

        //cardviewConductor
        cdw_conductor = (CardView) findViewById(R.id.conductor);
        img_imagen_conductor = (CircleImageView) findViewById(R.id.img_perfil_conductor);
        txv_nombre_conductor = (TextView) findViewById(R.id.id_conductor);
        txv_telefono_conductor = (TextView) findViewById(R.id.telefono_conductor);
        txv_rango_conductor = (TextView) findViewById(R.id.rango_conductor);
        txv_realisadas_conductor = (TextView) findViewById(R.id.realisadas_conductor);
        imb_cancelar_conductor = (ImageButton) findViewById(R.id.cancelar_btn);
        imb_cancelar_conductor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Cancelado();
            }
        });

        //cardviewConductor2
        cdw_conductor2 = (CardView) findViewById(R.id.conductor2);
        img_imagen_conductor2 = (CircleImageView) findViewById(R.id.img_perfil_conductor2);
        txv_nombre_conductor2 = (TextView) findViewById(R.id.id_conductor2);
        txv_info_conductor2 = (TextView) findViewById(R.id.info_conductor);
        imb_cancelar_conductor2 = (ImageButton) findViewById(R.id.cancelar_btn2);
        imb_cancelar_conductor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Cancelado();
            }
        });


        //BotonSolicitar
        btnSolicitarDeli = (AppCompatButton) findViewById(R.id.btnSolicitarDeli);
        btnSolicitarDeli.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!Common.isDriverFound) {
                    SolicitarDeli(FirebaseAuth.getInstance().getCurrentUser().getUid());
                } else {
                    SendRequesToDriver(DriverId);
                }
            }
        });

        CheckGPSStatus();
        verificarinternet();

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

//                            String json_lat_lng = new Gson().toJson(new LatLng(Common.MyLocation.getLatitude(), Common.MyLocation.getLongitude()));
                            String riderToken = FirebaseInstanceId.getInstance().getToken();
//                            Notification data = new Notification(riderToken, json_lat_lng); //enviar esto a la app driver
//                            Sender content = new Sender(token.getToken(), data); // enviar esta data al token

                            Map<String,String> content = new HashMap<>();
                            content.put("customer",riderToken);

                            if (Common.MyLocation != null) {
                                content.put("lat", String.valueOf(Common.MyLocation.getLatitude()));
                                content.put("lng", String.valueOf(Common.MyLocation.getLongitude()));
                            } else{
                                Log.e("ERROR" ,"No se concontro la localisacion");
                            }
                            DataMessage dataMessage = new DataMessage(token.getToken(),content);

                            mService.sendMessage(dataMessage)
                                    .enqueue(new Callback<FCMResponse>() {
                                        @Override
                                        public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                                            if (response.body().success == 1) {
                                                Toast.makeText(HomeBox.this, "Solicitud enviada.", Toast.LENGTH_SHORT).show();
                                                btnSolicitarDeli.setText("Esperando respuesta...");
                                                progressBar.setVisibility(View.VISIBLE);

                                            } else {
                                                Toast.makeText(HomeBox.this, "Error de solicitud.", Toast.LENGTH_SHORT).show();
                                                btnSolicitarDeli.setText(R.string.Buscar);
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

        final double latitude = Common.MyLocation.getLatitude();
        final double longitude = Common.MyLocation.getLongitude();


        DatabaseReference dbRequest = FirebaseDatabase.getInstance().getReference(Common.pickup_request_tbl);
        GeoFire mGeoFire = new GeoFire(dbRequest);

        mGeoFire.setLocation(uid, new GeoLocation(latitude, longitude),
                new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {

                    }
                });

        if (mUserMarker != null) {
            mUserMarker.remove();
        }
        mUserMarker = mapboxMap.addMarker(new MarkerOptions()
                .position(new LatLng(latitude, longitude))
                .title("Recojer aqui"));

        if (Common.isDriverFound) {
            mapboxMap.selectMarker(mUserMarker);
        }

        btnSolicitarDeli.setText("Obteniendo Deli...");

        BuscarConductor();

    }

    private void BuscarConductor() {

        DatabaseReference drivers = FirebaseDatabase.getInstance().getReference(Common.driver_tbl);
        GeoFire gfDrivers = new GeoFire(drivers);
        GeoQuery geoQuery = gfDrivers.queryAtLocation(new GeoLocation(Common.MyLocation.getLatitude(), Common.MyLocation.getLongitude()), Radius);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                // Si encuentra

                if (!Common.isDriverFound) {

                    Common.isDriverFound = true;
                    DriverId = key;
                    btnSolicitarDeli.setText("llamar deli");

                    cdw_conductor.setVisibility(View.VISIBLE);
                    setDataConductor(geoQuery);
                    Toast.makeText(HomeBox.this, "Encontrado", Toast.LENGTH_SHORT).show();

                    DriverLat = location.latitude;
                    DriverLng = location.longitude;
                    Common.CuandoEncuentra = true;

                    //animar camara a conductor
                    CameraPosition position = new CameraPosition.Builder()
                            .target(new LatLng(location.latitude, location.longitude)) // Sets the new camera position
                            .zoom(15) // Sets the zoom to level 10
                            .tilt(20) // Set the camera tilt to 20 degrees
                            .build(); // Builds the CameraPosition object from the builder

                    mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position));

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

                if (!Common.isDriverFound && Radius < Limite) {
                    Radius++;
                    BuscarConductor();

                } else {

                    if (!Common.isDriverFound) {
                        Toast.makeText(HomeBox.this, "No hay conductores cerca de ti.", Toast.LENGTH_SHORT).show();
                        btnSolicitarDeli.setText(R.string.Buscar);
                        removeSolicitud();
                        geoQuery.removeAllListeners();
                        //gfDrivers.removeLocation(DriverId);
                    }
                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

    }

    private void LoadAllAvailableDrivers() {

        if (mapboxMap != null) {
            mapboxMap.clear();
            mUserMarker = mapboxMap.addMarker(new MarkerOptions()
                    .position(new LatLng(Common.MyLocation.getLatitude(), Common.MyLocation.getLongitude()))
                    .title("Tu"));
        }


        // leer todos los conductores avilitados en un radio de 3 km.

        DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference(Common.driver_tbl);
        GeoFire gf = new GeoFire(driverLocation);

        GeoQuery geoQuery = gf.queryAtLocation(new GeoLocation(Common.MyLocation.getLatitude(), Common.MyLocation.getLongitude()), Distancia);
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

                                if (mapboxMap != null) {

                                    //Add driver to map
                                    mapboxMap.addMarker(new MarkerOptions()
                                            .position(new LatLng(location.latitude, location.longitude))
                                            .title(rider.getName())
                                            .snippet("Tel: " + rider.getPhone())
                                            .icon(icon));
                                }


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
    }

    private void displayLocation() {

        if (Common.MyLocation != null) {


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

            LoadAllAvailableDrivers();

            if (Common.MyLocation != null) {
                if (!Common.CuandoEncuentra) {
                    enfocateLocation();
                }
            }

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

        /////////////////////////////////////////
        locationEngine.setInterval(UPDATE_INTERVAL);
        locationEngine.setFastestInterval(FASTEST_INTERVAL);
        locationEngine.setSmallestDisplacement(DISPLACEMENT);
        /////////////////////////////////////////

        locationEngine.activate();

        Location lastLocation = locationEngine.getLastLocation();
        if (lastLocation != null) {
            Common.MyLocation = lastLocation;
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
                        logout();
                    }
                });
                dialogo.setNegativeButton("Cancelado", new DialogInterface.OnClickListener() {
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
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            Common.MyLocation = location;
            displayLocation();
            try {
                getLocation();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    //////////////////////////// MIOS

    private void getLocation() throws IOException {

        double latitude = Common.MyLocation.getLatitude();
        double longitude = Common.MyLocation.getLongitude();

        List<Address> addresses;
        geocoder = new Geocoder(this, Locale.getDefault());

        addresses = geocoder.getFromLocation(latitude, longitude, 1);

        String txtLocationn = addresses.get(0).getAddressLine(0);

        txtLocation.setText(txtLocationn);
    }

    private void logout() {

        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public void CheckGPSStatus() {

        //verificar si el gps esta activo
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            AlertNoGps();
        }
    }

    private void verificarinternet() {
        //verificar internet
        if (InternetConnection.checkConnection(c)) {
            // Its Available...

            //verificar si hay internet con ping
            if (InternetConnection.internetIsConnected(c)) {
                enableLocationPlugin();
                connected = true;
                updateFirebaseToken();


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

    private static void removeSolicitud() {

        mdatabaseRemove = FirebaseDatabase.getInstance().getReference(Common.pickup_request_tbl);
        mdatabaseRemove.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).removeValue();

    }

    private void setDataConductor(GeoQuery geoQuery) {

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                //Use key to get email from table Drivers
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Common.user_driver_tbl);
                ref.child(key).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        Rider rider = dataSnapshot.getValue(Rider.class);

                        //Add driver  data to card
                        txv_nombre_conductor.setText(rider.getName());
                        txv_telefono_conductor.setText(rider.getPhone());

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

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

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

    public static void Cancelado() {

        progressBar.setVisibility(View.INVISIBLE);
        Common.isDriverFound = false;
        Common.CuandoEncuentra = false;
        cdw_conductor.setVisibility(View.INVISIBLE);
        btnSolicitarDeli.setText(R.string.Buscar);
        removeSolicitud();

    }

    public static void Aceptado() {

        progressBar.setVisibility(View.INVISIBLE);
        Common.onService = true;
        cdw_conductor.setVisibility(View.INVISIBLE);
        cdw_conductor2.setVisibility(View.VISIBLE);
        btnSolicitarDeli.setVisibility(View.GONE);
        Onservice();


    }

    private static void Onservice() {



    }

    private void enfocateLocation() {

        if (Common.MyLocation != null) {
            if (mapboxMap != null) {
                CameraPosition position = new CameraPosition.Builder()
                        .target(new LatLng(Common.MyLocation.getLatitude(), Common.MyLocation.getLongitude())) // Sets the new camera position
                        .zoom(15) // Sets the zoom to level 10
                        .tilt(20) // Set the camera tilt to 20 degrees
                        .build(); // Builds the CameraPosition object from the builder

                mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position));
            }
        }
    }
}