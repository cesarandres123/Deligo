package com.vecolsoft.deligo.Activitys;

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.leo.simplearcloader.ArcConfiguration;
import com.leo.simplearcloader.SimpleArcDialog;
import com.vecolsoft.deligo.Common.Common;
import com.vecolsoft.deligo.Modelo.Rider;
import com.vecolsoft.deligo.R;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class PerfilActivity extends AppCompatActivity {

    private DatabaseReference mDatabase;
    private CircleImageView img_perfil;
    private TextView nombre_perfil;
    private TextView email_perfil;
    private TextView telefono_perfil;
    private RelativeLayout changePassPerfil;

    private int mColors[] = {Color.parseColor("#009688"), // prymary
            Color.parseColor("#00796B"), // prrymary dark
            Color.parseColor("#607D8B"), // acent
            Color.parseColor("#FFFFFF")};// blanco

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

        setContentView(R.layout.activity_perfil);

        img_perfil = (CircleImageView) findViewById(R.id.img_perfil);
        nombre_perfil = (TextView) findViewById(R.id.nombre_perfil);
        email_perfil = (TextView) findViewById(R.id.email_perfil);
        telefono_perfil = (TextView) findViewById(R.id.telefono_perfil);
        changePassPerfil = (RelativeLayout) findViewById(R.id.changePassPerfil);


        mDatabase = FirebaseDatabase.getInstance().getReference(Common.user_rider_tbl);
        mDatabase.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Rider rider = dataSnapshot.getValue(Rider.class);

                        nombre_perfil.setText(rider.getName());
                        email_perfil.setText(rider.getEmail());
                        telefono_perfil.setText(rider.getPhone());
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });


        changePassPerfil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialogChangepwd();
            }
        });


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle("Perfil");

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void showDialogChangepwd() {

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(PerfilActivity.this);
        alertDialog.setTitle("CAMBIAR CONTRASEÑA");

        LayoutInflater inflater = this.getLayoutInflater();
        View Layout_pwd = inflater.inflate(R.layout.layout_change_pwd, null);

        final EditText edtPassword = Layout_pwd.findViewById(R.id.edtPassword);
        final EditText edtNewPassword = Layout_pwd.findViewById(R.id.edtNewPassword);
        final EditText edtRepeatPassword = Layout_pwd.findViewById(R.id.edtRepeatPassword);

        alertDialog.setView(Layout_pwd);

        alertDialog.setPositiveButton("CAMBIAR", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                //Dialogo de Carga
                final SimpleArcDialog CargaDialog = new SimpleArcDialog(PerfilActivity.this);
                ArcConfiguration configuration = new ArcConfiguration(PerfilActivity.this);
                configuration.setText("Cargando..");
                configuration.setColors(mColors);

                CargaDialog.setConfiguration(configuration);
                CargaDialog.show();

                if (edtNewPassword.getText().toString().equals(edtRepeatPassword.getText().toString())) {
                    String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                    //get auth credenciale from the user  for re-autentication
                    //examample whit only emeail
                    AuthCredential credential = EmailAuthProvider.getCredential(email, edtPassword.getText().toString());
                    FirebaseAuth.getInstance().getCurrentUser()
                            .reauthenticate(credential)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        FirebaseAuth.getInstance().getCurrentUser()
                                                .updatePassword(edtRepeatPassword.getText().toString())
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {

                                                        if (task.isSuccessful()) {

                                                            //update rider information password column
                                                            Map<String, Object> password = new HashMap<>();

                                                            password.put("password", edtRepeatPassword.getText().toString());
                                                            DatabaseReference RiderInformation = FirebaseDatabase.getInstance().getReference(Common.user_rider_tbl);


                                                            RiderInformation.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                                                    .updateChildren(password)
                                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task) {

                                                                            if (task.isSuccessful())
                                                                                Toast.makeText(PerfilActivity.this, "Contraseña Cambiada!", Toast.LENGTH_SHORT).show();
                                                                            else
                                                                                Toast.makeText(PerfilActivity.this, "Contraseña Cambiada pero no actualised a la informacion del usuario", Toast.LENGTH_LONG).show();

                                                                            CargaDialog.dismiss();


                                                                        }
                                                                    });


                                                        } else {
                                                            CargaDialog.dismiss();
                                                            Toast.makeText(PerfilActivity.this, "No se pudo cambiar la contraseña", Toast.LENGTH_SHORT).show();

                                                        }

                                                    }
                                                });
                                    } else {
                                        CargaDialog.dismiss();
                                        Toast.makeText(PerfilActivity.this, "Wrong old password", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });

                } else {
                    CargaDialog.dismiss();
                    Toast.makeText(PerfilActivity.this, "Contraseñas no coinciden", Toast.LENGTH_SHORT).show();
                }
            }
        });

        alertDialog.setNegativeButton("CANCELAR", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        alertDialog.show();

    }

}
