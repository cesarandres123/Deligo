package com.vecolsoft.deligo.Activitys;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.leo.simplearcloader.ArcConfiguration;
import com.leo.simplearcloader.SimpleArcDialog;
import com.vecolsoft.deligo.Common.Common;
import com.vecolsoft.deligo.Modelo.Rider;
import com.vecolsoft.deligo.R;
import com.vecolsoft.deligo.Utils.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class PerfilActivity extends AppCompatActivity {

    private CircleImageView img_perfil;
    private TextView nombre_perfil;
    private TextView email_perfil;
    private TextView telefono_perfil;
    private RelativeLayout changePassPerfil;

    private int mColors[] = {Color.parseColor("#009688"), // prymary
            Color.parseColor("#00796B"), // prrymary dark
            Color.parseColor("#607D8B"), // acent
            Color.parseColor("#FFFFFF")};// blanco

    //firebase Storage declaration
    FirebaseStorage storage;
    StorageReference storageReference;

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

        //ints storage
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        img_perfil = (CircleImageView) findViewById(R.id.img_perfil);

        //load Avatar
        if (Common.CurrentUser.getAvatarUrl() != null && !TextUtils.isEmpty(Common.CurrentUser.getAvatarUrl())) {

            Glide.with(this)
                    .load(Common.CurrentUser.getAvatarUrl())
                    .into(img_perfil);
        }

        img_perfil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                choseImage();
            }
        });

        nombre_perfil = (TextView) findViewById(R.id.nombre_perfil);
        email_perfil = (TextView) findViewById(R.id.email_perfil);
        telefono_perfil = (TextView) findViewById(R.id.telefono_perfil);
        changePassPerfil = (RelativeLayout) findViewById(R.id.changePassPerfil);

        nombre_perfil.setText(Common.CurrentUser.getName());
        email_perfil.setText(Common.CurrentUser.getEmail());
        telefono_perfil.setText(Common.CurrentUser.getPhone());
        changePassPerfil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialogChangepwd();
            }
        });


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_perfil);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle("Perfil");

    }

    private void choseImage() {

        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Selecciona una imagen"), Common.PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Common.PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri saveUri = data.getData();
            if (saveUri != null) {
                final ProgressDialog mDialog = new ProgressDialog(this);
                mDialog.setMessage("ACTUALIZANDO...");
                mDialog.show();

                String imageName = UUID.randomUUID().toString(); //Ramdom name image upload
                final StorageReference imageFolder = storageReference.child("images/" + imageName);
                imageFolder.putFile(saveUri)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                mDialog.dismiss();

                                imageFolder.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {

                                        Map<String, Object> avatarUpdate = new HashMap<>();
                                        avatarUpdate.put("avatarUrl", uri.toString());

                                        DatabaseReference RiderInformation = FirebaseDatabase.getInstance().getReference(Common.user_rider_tbl);
                                        RiderInformation.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                                .updateChildren(avatarUpdate)
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {

                                                        if (task.isSuccessful())
                                                            Toast.makeText(PerfilActivity.this, "ACTUALIZADO!", Toast.LENGTH_SHORT).show();
                                                        else
                                                            Toast.makeText(PerfilActivity.this, "ERROR.", Toast.LENGTH_SHORT).show();

                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        Toast.makeText(PerfilActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();

                                                    }
                                                });


                                    }
                                });
                            }
                        })
                        .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                double progess = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                                mDialog.setMessage("SUBIENDO " + progess + "%");
                            }
                        });

            }

        }
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

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

}
