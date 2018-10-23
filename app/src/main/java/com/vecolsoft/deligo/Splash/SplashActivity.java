package com.vecolsoft.deligo.Splash;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.leo.simplearcloader.ArcConfiguration;
import com.leo.simplearcloader.SimpleArcDialog;
import com.vecolsoft.deligo.Activitys.HomeBox;
import com.vecolsoft.deligo.Activitys.MainActivity;
import com.vecolsoft.deligo.Common.Common;
import com.vecolsoft.deligo.Modelo.Rider;
import com.vecolsoft.deligo.R;
import com.vecolsoft.deligo.Utils.InternetConnection;
import com.vecolsoft.deligo.Utils.Utils;

public class SplashActivity extends AppCompatActivity {

    FirebaseAuth auth;
    FirebaseDatabase database;
    DatabaseReference ref;

    //verificar internet
    boolean connected = false;
    //Contexto
    private Context c = this;

    private int mColors[] = {Color.parseColor("#009688"), // prymary
            Color.parseColor("#00796B"), // prrymary dark
            Color.parseColor("#607D8B"), // acent
            Color.parseColor("#FFFFFF")};// blanco

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Bolquear rotacion
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);

        //Firebase Views
        auth = FirebaseAuth.getInstance();

        database = FirebaseDatabase.getInstance();

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {

            ref = database.getReference(Common.user_rider_tbl)
                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        }

        //Dialogo de Carga
        final SimpleArcDialog CargaDialog = new SimpleArcDialog(this);
        ArcConfiguration configuration = new ArcConfiguration(this);
        configuration.setText("Cargando..");
        configuration.setColors(mColors);


        CargaDialog.setConfiguration(configuration);
        CargaDialog.setCancelable(false);
        CargaDialog.show();


        final Intent intentMain = new Intent(this, MainActivity.class);
        final Intent intentHome = new Intent(this, HomeBox.class);
        intentHome.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        //post delay para iniciar
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                //verificar internet
                if (InternetConnection.checkConnection(c)) {
                    // Its Available...

                    //verificar si hay internet con ping
                    if (InternetConnection.internetIsConnected(c)) {

                        if (auth.getCurrentUser() != null) {


                            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {

                                    Common.CurrentUser = dataSnapshot.getValue(Rider.class);
                                    CargaDialog.dismiss();
                                    startActivity(intentHome);
                                    finish();
                                    connected = true;
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });




                        } else {
                            CargaDialog.dismiss();
                            startActivity(intentMain);
                            finish();
                        }

                    } else {

                        AlertDialog.Builder dialogo = new AlertDialog.Builder(c);
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

                    AlertDialog.Builder dialogo = new AlertDialog.Builder(c);
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
        }, 1000);


    }

}
