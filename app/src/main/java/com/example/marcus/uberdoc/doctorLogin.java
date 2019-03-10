package com.example.marcus.uberdoc;

import android.support.v7.app.AppCompatActivity;
import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class doctorLogin extends AppCompatActivity {

    EditText dEmail, dPassword;
    Button login, register;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_login);


        FirebaseApp.initializeApp(this);

        mAuth = FirebaseAuth.getInstance();
        firebaseAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if(user!=null)
                {
                    Intent intent = new Intent(doctorLogin.this, DoctorMapActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        };

        dEmail = (EditText) findViewById(R.id.docEmail);
        dPassword = (EditText) findViewById(R.id.docPassword);

        login = (Button) findViewById(R.id.dLogin);
        register = (Button) findViewById(R.id.dRegister);

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(TextUtils.isEmpty(dEmail.getText().toString())){
                    Toast.makeText(doctorLogin.this,"Please Enter Email Address",Toast.LENGTH_SHORT)
                            .show();
                    return;
                }

                if(TextUtils.isEmpty(dPassword.getText().toString())) {
                    Toast.makeText(doctorLogin.this, "Please Enter Password", Toast.LENGTH_SHORT)
                            .show();
                    return;
                }

                final String email = dEmail.getText().toString();
                final String password = dPassword.getText().toString();
                mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(doctorLogin.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(!task.isSuccessful())
                        {
                            Toast.makeText(doctorLogin.this, "Error on Login", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = dEmail.getText().toString();
                final String password = dPassword.getText().toString();
                mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(doctorLogin.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(!task.isSuccessful())
                        {
                            Toast.makeText(doctorLogin.this, "Error on Sign Up", Toast.LENGTH_SHORT).show();
                        }
                        else
                        {
                            String doc_ID = mAuth.getCurrentUser().getUid();
                            DatabaseReference current_user_db = FirebaseDatabase.getInstance().getReference().child("users").child("doctor").child(doc_ID);
                            current_user_db.setValue(true);
                        }
                    }
                });
            }
        });



    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(firebaseAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAuth.removeAuthStateListener(firebaseAuthListener);
    }

}



