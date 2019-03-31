package com.example.marcus.uberdoc;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class DoctorSettings extends AppCompatActivity {


    private EditText dName, dNumber;
    private Button dSave, dBack;

    private FirebaseAuth mAuth;
    private DatabaseReference doctorDatabase;
    private String doctorID, dNameStr, dNumberStr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_settings);

        dName = (EditText) findViewById(R.id.doctorName);
        dNumber = (EditText) findViewById(R.id.doctorNumber);

        dSave = (Button) findViewById(R.id.save);
        dBack = (Button) findViewById(R.id.back);

        mAuth = FirebaseAuth.getInstance();
        doctorID = mAuth.getCurrentUser().getUid();

        doctorDatabase = FirebaseDatabase.getInstance().getReference().child("users").child("doctor").child(doctorID);

        getPatientData();

        dSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveData();
            }
        });

        dBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    private void getPatientData()
    {
        doctorDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0)
                {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();

                    if(map.get("name") != null)
                    {
                        dNameStr = map.get("name").toString();
                        dName.setText(dNameStr);
                    }
                    if(map.get("number") != null)
                    {
                        dNumberStr = map.get("number").toString();
                        dNumber.setText(dNumberStr);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    private void saveData()
    {
        dNameStr = dName.getText().toString();
        dNumberStr = dNumber.getText().toString();

        Map newData = new HashMap();
        newData.put("name", dNameStr);
        newData.put("number", dNumberStr);

        doctorDatabase.updateChildren(newData);

        finish();
    }
}
