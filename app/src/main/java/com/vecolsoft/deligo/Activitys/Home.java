package com.vecolsoft.deligo.Activitys;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.google.gson.Gson;
import com.vecolsoft.deligo.Common.Common;
import com.vecolsoft.deligo.Modelo.FCMResponse;
import com.vecolsoft.deligo.Modelo.Notification;
import com.vecolsoft.deligo.Modelo.Rider;
import com.vecolsoft.deligo.Modelo.Sender;
import com.vecolsoft.deligo.Modelo.Token;
import com.vecolsoft.deligo.R;
import com.vecolsoft.deligo.Remote.IFCMService;
import com.vecolsoft.deligo.Utils.InternetConnection;
import com.vecolsoft.deligo.Utils.Utils;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class Home extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private SharedPreferences prefs;


    //elementos del toolbar
    private Toolbar toolbar;
    private CircleImageView circleImageView;
    private RelativeLayout perfil;
    /////////////////////////////////

    //ELEMENTOS
    private GoogleMap mMap;
    private AppCompatButton btnSolicitarDeli;

    //Play servivios
    private static final int MI_PERMISION_REQUEST_CODE = 7000;
    private static final int PLAY_SERVICE_RES_REQUEST = 7001;
    private LocationRequest mLocationrequest;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private LocationManager locationManager;

    private static int UPDATE_INTERVAL = 5000;
    private static int FASTED_INTERVAL = 3000;
    private static int DISPLACEMENT = 10;

    DatabaseReference drivers;
    GeoFire geoFire;

    Marker mUserMarker;

    boolean isDriverFound = false;
    String DriverId = "";
    int Radius = 1; //Radio de 1 Kilometro(s)
    int Distancia = 1; //
    private static final int Limite = 3 ;

    //enviar alerta
    IFCMService mService;

    //Contexto
    private Context c = this;

    //verificar internet
    boolean connected = false;

    //Sistema de precencia
    DatabaseReference DriversAvailable;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Bolquear rotacion
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);



        //      Fuente
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/Roboto-Regular.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build());

        setContentView(R.layout.activity_home);

        mService = Common.getFCMService();


        prefs = getSharedPreferences("datos", Context.MODE_PRIVATE);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //perfil
        perfil = (RelativeLayout) findViewById(R.id.perfil);
        perfil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startActivity(new Intent(Home.this, PerfilActivity.class));
                //overridePendingTransition( R.anim.slide_in_up, R.anim.slide_out_up );
            }
        });

        //Toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        //BotonSolicitar
        btnSolicitarDeli = (AppCompatButton) findViewById(R.id.btnSolicitarDeli);
        btnSolicitarDeli.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!isDriverFound){
                    SolicitarDeli(FirebaseAuth.getInstance().getCurrentUser().getUid());
                }else {
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
                setUpLocation();
                connected = true;


            }else {

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
                        for (DataSnapshot postSnapShot:dataSnapshot.getChildren())
                        {
                            Token token = postSnapShot.getValue(Token.class);

                            String json_lat_lng = new Gson().toJson(new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude()));
                            String riderToken = FirebaseInstanceId.getInstance().getToken();
                            Notification data = new Notification(riderToken,json_lat_lng); //enviar esto a la app driver
                            Sender content = new Sender(token.getToken(),data); // enviar esta data al token

                            mService.sendMessage(content)
                                    .enqueue(new Callback<FCMResponse>() {
                                        @Override
                                        public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                                            if (response.body().success == 1 ) {
                                                Toast.makeText(Home.this, "Solicitud enviada.", Toast.LENGTH_SHORT).show();
                                            }else {
                                                Toast.makeText(Home.this, "Error de solicitud.", Toast.LENGTH_SHORT).show();
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<FCMResponse> call, Throwable t) {
                                            Log.d("ERROR",t.getMessage());
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

        final double latitude = mLastLocation.getLatitude();
        final double longitude = mLastLocation.getLongitude();


        DatabaseReference dbRequest = FirebaseDatabase.getInstance().getReference(Common.pickup_request_tbl);
        GeoFire mGeoFire = new GeoFire(dbRequest);
        mGeoFire.setLocation(uid, new GeoLocation(latitude, longitude),
                new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                    }
                });


        if (mUserMarker.isVisible()) {
            mUserMarker.remove();
            mUserMarker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(latitude, longitude))
                    .title("Recojer aqui"));

            mUserMarker.showInfoWindow();

            btnSolicitarDeli.setText("obteniendo Deli");
        }

        BuscarConductor();

    }

    private void BuscarConductor() {
        DatabaseReference drivers = FirebaseDatabase.getInstance().getReference(Common.driver_tbl);
        GeoFire gfDrivers = new GeoFire(drivers);

        GeoQuery geoQuery = gfDrivers.queryAtLocation(new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()), Radius);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                // Si encuentra

                if (!isDriverFound) {

                    isDriverFound = true;
                    DriverId = key;
                    btnSolicitarDeli.setText("llamar deli");
                    Toast.makeText(Home.this, "Encontrado", Toast.LENGTH_SHORT).show();

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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MI_PERMISION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (checkPlayServices()) {
                        buitGoogleApiClient();
                        createLocationResquest();
                        displayLocation();

                    }
                }
                break;
        }
    }

    private void setUpLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
            }, MI_PERMISION_REQUEST_CODE);
        } else {
            if (checkPlayServices()) {

                buitGoogleApiClient();
                createLocationResquest();
                displayLocation();

            }
        }
    }

    private void displayLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {


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


            final double latitude = mLastLocation.getLatitude();
            final double longitude = mLastLocation.getLongitude();

            //añadir marcador marker
            if (mUserMarker != null)

                mUserMarker.remove(); // remove viejo marcador

            mUserMarker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(latitude, longitude))
                    .title(String.format("Tu")));

            //Mover la camara a esta pocicion
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 16.5f));

            LoadAllAvailableDrivers();


        } else {
            Log.d("ERROR", "No se puede obtener la localisacion.");
        }
    }

    private void LoadAllAvailableDrivers() {

        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude()))
                        .title("Tu"));

        // leer todos los conducteres avilitados en un radio de 3 km.

        DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference(Common.driver_tbl);
        GeoFire gf = new GeoFire(driverLocation);

        GeoQuery geoQuery = gf.queryAtLocation(new GeoLocation(mLastLocation.getLatitude(),mLastLocation.getLongitude()),Distancia);
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

                                //Add driver to map
                                mMap.addMarker(new MarkerOptions()
                                            .position(new LatLng(location.latitude,location.longitude))
                                            .flat(true)
                                            .title(rider.getName())
                                            .snippet(rider.getPhone())
                                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.circlemo)));

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

    private void createLocationResquest() {
        mLocationrequest = new LocationRequest();
        mLocationrequest.setInterval(UPDATE_INTERVAL);
        mLocationrequest.setFastestInterval(FASTED_INTERVAL);
        mLocationrequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationrequest.setSmallestDisplacement(DISPLACEMENT);
    }

    private void buitGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICE_RES_REQUEST).show();
            } else {
                Toast.makeText(this, "Este dispositivo no es soportado.", Toast.LENGTH_SHORT).show();
                finish();
            }
            return false;
        }

        return true;
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
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setTrafficEnabled(false);
        mMap.setIndoorEnabled(false);
        mMap.setBuildingsEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setRotateGesturesEnabled(false);

        //Add move camera to colombia
        LatLng colombia = new LatLng(5, -74);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(colombia, 6));


    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();
        startLocationUpdates();
    }


    private void startLocationUpdates() {

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationrequest, this);

    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        displayLocation();

    }

    private void logout() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    public void CheckGPSStatus() {

        //verificar si el gps esta activo
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if ( !locationManager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
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
}
