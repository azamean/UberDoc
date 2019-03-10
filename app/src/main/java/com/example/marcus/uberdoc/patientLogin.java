package com.example.marcus.uberdoc;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Bundle;
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

public class patientLogin extends AppCompatActivity {


    EditText pEmail, pPassword;
    Button login, register;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener firebaseAuthListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_login);

        FirebaseApp.initializeApp(this);

        mAuth = FirebaseAuth.getInstance();
        firebaseAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if(user!=null)
                {
                    Intent intent = new Intent(patientLogin.this, PatientMapActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        };

        pEmail = (EditText) findViewById(R.id.patEmail);
        pPassword = (EditText) findViewById(R.id.patPassword);

        login = (Button) findViewById(R.id.pLogin);
        register = (Button) findViewById(R.id.pRegister);

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(TextUtils.isEmpty(pEmail.getText().toString())){
                    Toast.makeText(patientLogin.this,"Please Enter Email Address",Toast.LENGTH_SHORT)
                            .show();
                    return;
                }

                if(TextUtils.isEmpty(pPassword.getText().toString())) {
                    Toast.makeText(patientLogin.this, "Please Enter Password", Toast.LENGTH_SHORT)
                            .show();
                    return;
                }

                final String email = pEmail.getText().toString();
                final String password = pPassword.getText().toString();
                mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(patientLogin.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(!task.isSuccessful())
                        {
                            Toast.makeText(patientLogin.this, "Error on Login", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = pEmail.getText().toString();
                final String password = pPassword.getText().toString();
                mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(patientLogin.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(!task.isSuccessful())
                        {
                            Toast.makeText(patientLogin.this, "Error on Sign Up", Toast.LENGTH_SHORT).show();
                        }
                        else
                        {
                            String pat_ID = mAuth.getCurrentUser().getUid();
                            DatabaseReference current_user_db = FirebaseDatabase.getInstance().getReference().child("users").child("patient").child(pat_ID);
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
