package com.vecolsoft.deligo.Activitys;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.leo.simplearcloader.ArcConfiguration;
import com.leo.simplearcloader.SimpleArcDialog;
import com.vecolsoft.deligo.Common.Common;
import com.vecolsoft.deligo.R;

import java.util.regex.Pattern;

import com.vecolsoft.deligo.Modelo.Rider;
import com.vecolsoft.deligo.Utils.Utils;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences prefs;

    Button btnForgpass, btnSignIn, btnRegister;
    RelativeLayout rootLayout;

    private int mColors[] = {Color.parseColor("#009688"), // prymary
            Color.parseColor("#00796B"), // prrymary dark
            Color.parseColor("#607D8B"), // acent
            Color.parseColor("#FFFFFF")};// blanco

    FirebaseAuth auth;
    FirebaseDatabase db;
    DatabaseReference users;

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

        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("datos",Context.MODE_PRIVATE);

        //Init View
        btnSignIn = findViewById(R.id.btnSignIn);
        btnRegister = findViewById(R.id.btnRegister);
        btnForgpass = findViewById(R.id.btnForgpass);
        rootLayout = findViewById(R.id.rootLayout);

        //Firebase Views
        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();
        users = db.getReference(Common.user_rider_tbl);

        //Sharepreference
        setCredentialsifExist();


        //Eventos

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogoDeRegistro();
            }
        });

        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Ingresar();
            }
        });

        btnForgpass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RecuperarContrasenia();
            }
        });
    }

    private boolean validarEmail(String email) {
        Pattern pattern = Patterns.EMAIL_ADDRESS;
        return pattern.matcher(email).matches();
    }

    private void Ingresar() {

        final EditText edtEmail = rootLayout.findViewById(R.id.edtEmail);
        final EditText edtPassword = rootLayout.findViewById(R.id.edtPassword);

        // validation de campos

        if (!validarEmail(edtEmail.getText().toString())) {
            Snackbar.make(rootLayout, "Ingrese un email valido", Snackbar.LENGTH_SHORT)
                    .show();
            return;
        }

        if (TextUtils.isEmpty(edtPassword.getText().toString())) {
            Snackbar.make(rootLayout, "Ingrese una contraseña", Snackbar.LENGTH_SHORT)
                    .show();
            return;
        }

        if (edtPassword.getText().toString().length() < 6) {
            Snackbar.make(rootLayout, "La contraseña debe ser mayor a 6.", Snackbar.LENGTH_SHORT)
                    .show();
            return;
        }

        //Dialogo de Carga
        final SimpleArcDialog CargaDialog = new SimpleArcDialog(this);
        ArcConfiguration configuration = new ArcConfiguration(this);
        configuration.setText("Cargando..");
        configuration.setColors(mColors);


        CargaDialog.setConfiguration(configuration);
        CargaDialog.setCancelable(false);
        CargaDialog.show();

        btnSignIn.setEnabled(false);


        //Ingresar

        auth.signInWithEmailAndPassword(edtEmail.getText().toString(), edtPassword.getText().toString())
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        CargaDialog.dismiss();
                        startActivity(new Intent(MainActivity.this, HomeBox.class));
                        saveOnPreferences(edtEmail.getText().toString(),edtPassword.getText().toString());
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        CargaDialog.dismiss();
                        Snackbar.make(rootLayout, "Error al ingresar " + e.getMessage(), Snackbar.LENGTH_SHORT).show();
                        btnSignIn.setEnabled(true);
                    }
                });

    }

    private void DialogoDeRegistro() {

        AlertDialog.Builder dialogo = new AlertDialog.Builder(this);
        dialogo.setTitle("Registro");
        dialogo.setCancelable(false);
        dialogo.setIcon(R.drawable.ic_assignment);


        LayoutInflater inflador = LayoutInflater.from(this);
        View register_layout = inflador.inflate(R.layout.layout_registro, null);

        final EditText edtNameR = register_layout.findViewById(R.id.edtNameR);
        final EditText edtEmailR = register_layout.findViewById(R.id.edtEmailR);
        final EditText edtPasswordR = register_layout.findViewById(R.id.edtPasswordR);
        final EditText edtPhoneR = register_layout.findViewById(R.id.edtPhoneR);

        //Convertidor de campo telefono a formato (123)456-7890
        edtPhoneR.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // TODO Auto-generated method stub
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub
            }

            @Override
            public void afterTextChanged(Editable s) {
                String text = edtPhoneR.getText().toString();
                int textLength = edtPhoneR.getText().length();
                if (text.endsWith("-") || text.endsWith(" ") || text.endsWith(" "))
                    return;
                if (textLength == 1) {
                    if (!text.contains("(")) {
                        edtPhoneR.setText(new StringBuilder(text).insert(text.length() - 1, "(").toString());
                        edtPhoneR.setSelection(edtPhoneR.getText().length());
                    }
                } else if (textLength == 5) {
                    if (!text.contains(")")) {
                        edtPhoneR.setText(new StringBuilder(text).insert(text.length() - 1, ")").toString());
                        edtPhoneR.setSelection(edtPhoneR.getText().length());
                    }
                } else if (textLength == 6) {
                    edtPhoneR.setText(new StringBuilder(text).insert(text.length() - 1, " ").toString());
                    edtPhoneR.setSelection(edtPhoneR.getText().length());
                } else if (textLength == 10) {
                    if (!text.contains("-")) {
                        edtPhoneR.setText(new StringBuilder(text).insert(text.length() - 1, "-").toString());
                        edtPhoneR.setSelection(edtPhoneR.getText().length());
                    }
                } else if (textLength == 15) {
                    if (text.contains("-")) {
                        edtPhoneR.setText(new StringBuilder(text).insert(text.length() - 1, "-").toString());
                        edtPhoneR.setSelection(edtPhoneR.getText().length());
                    }
                } else if (textLength == 18) {
                    if (text.contains("-")) {
                        edtPhoneR.setText(new StringBuilder(text).insert(text.length() - 1, "-").toString());
                        edtPhoneR.setSelection(edtPhoneR.getText().length());
                    }
                }
            }
        });

        //fin del Convertidor de campo telefono a formato (123)456-7890

        dialogo.setView(register_layout);

        //Botones
        dialogo.setPositiveButton("Registrar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                //Validar campos

                if (!validarEmail(edtEmailR.getText().toString())) {
                    Snackbar.make(rootLayout, "Ingrese un email valido", Snackbar.LENGTH_SHORT)
                            .show();
                    return;
                }

                if (TextUtils.isEmpty(edtNameR.getText().toString())) {
                    Snackbar.make(rootLayout, "Ingresa un nombre.", Snackbar.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(edtPasswordR.getText().toString())) {
                    Snackbar.make(rootLayout, "Ingresa una contraseña.", Snackbar.LENGTH_SHORT).show();
                    return;
                }

                if (edtPasswordR.getText().toString().length() < 6) {
                    Snackbar.make(rootLayout, "La contraseña debe ser mayor a 6.", Snackbar.LENGTH_SHORT).show();
                    return;
                }

                //Dialogo de Carga
                final SimpleArcDialog CargaDialog = new SimpleArcDialog(MainActivity.this);
                ArcConfiguration configuration = new ArcConfiguration(MainActivity.this);
                configuration.setText("Cargando..");
                configuration.setColors(mColors);

                CargaDialog.setConfiguration(configuration);
                CargaDialog.show();

                //Registro de nuevo usuario
                auth.createUserWithEmailAndPassword(edtEmailR.getText().toString(), edtPasswordR.getText().toString())
                        .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                            @Override
                            public void onSuccess(AuthResult authResult) {

                                //guardar en la base de datos
                                Rider user = new Rider();
                                user.setEmail(edtEmailR.getText().toString());
                                user.setName(edtNameR.getText().toString());
                                user.setPassword(edtPasswordR.getText().toString());
                                user.setPhone(edtPhoneR.getText().toString());

                                //usar el email como clave *user.getEmail()* posible uso de nombre.
                                users.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                        .setValue(user)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                CargaDialog.dismiss();
                                                Toast.makeText(MainActivity.this, "Registro completado.", Toast.LENGTH_SHORT).show();
                                                startActivity(new Intent(MainActivity.this, HomeBox.class));
                                                finish();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                CargaDialog.dismiss();
                                                Snackbar.make(rootLayout, "Registro fallido." + e.getMessage(), Snackbar.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                CargaDialog.dismiss();
                                Snackbar.make(rootLayout, "Registro fallido." + e.getMessage(), Snackbar.LENGTH_SHORT).show();

                            }
                        });


            }
        });

        dialogo.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        dialogo.show();


    }

    private void RecuperarContrasenia() {

        AlertDialog.Builder dialogo = new AlertDialog.Builder(this);
        dialogo.setTitle("Recuperar contraseña");
        dialogo.setCancelable(false);
        dialogo.setIcon(R.drawable.ic_key);

        LayoutInflater inflador = LayoutInflater.from(this);
        View forgot_psw_layout = inflador.inflate(R.layout.layout_recuperar_psw, null);

        final EditText edtForgotEmail = forgot_psw_layout.findViewById(R.id.edtForgotEmail);
        dialogo.setView(forgot_psw_layout);

        //SET BUTTON
        dialogo.setPositiveButton("RESTABLECER", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {

                //Dialogo de Carga
                final SimpleArcDialog CargaDialog = new SimpleArcDialog(MainActivity.this);
                ArcConfiguration configuration = new ArcConfiguration(MainActivity.this);
                configuration.setText("Cargando..");
                configuration.setColors(mColors);

                CargaDialog.setConfiguration(configuration);
                CargaDialog.show();


                auth.sendPasswordResetEmail(edtForgotEmail.getText().toString().trim())
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                dialog.dismiss();
                                CargaDialog.dismiss();
                                Snackbar.make(rootLayout, "Se ha enviado un link a tu correo. ", Snackbar.LENGTH_SHORT)
                                        .show();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        dialog.dismiss();
                        CargaDialog.dismiss();
                        Snackbar.make(rootLayout, "" + e.getMessage(), Snackbar.LENGTH_SHORT)
                                .show();
                    }
                });
            }
        });

        dialogo.setNegativeButton("CANCELAR", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        dialogo.show();

    }


    private void saveOnPreferences(String edtEmail ,String edtPassword) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("email",edtEmail);
        editor.putString("pass",edtPassword);
        editor.apply();
    }
    private void setCredentialsifExist() {

        final EditText edtEmail = rootLayout.findViewById(R.id.edtEmail);
        final EditText edtPassword = rootLayout.findViewById(R.id.edtPassword);

        String email = Utils.getUserEmailPrefes(prefs);
        String password = Utils.getUserPassPrefes(prefs);

        if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)) {
            edtEmail.setText(email);
            edtPassword.setText(password);
        }

    }

}
