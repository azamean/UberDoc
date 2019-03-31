package com.example.marcus.uberdoc;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
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

public class PatientSettings extends AppCompatActivity {


    private EditText pName, pNumber;
    private Button pSave, pBack;

    private FirebaseAuth mAuth;
    private DatabaseReference patientDatabase;
    private String patientID, pNameStr, pNumberStr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_settings);

        pName = (EditText) findViewById(R.id.patientName);
        pNumber = (EditText) findViewById(R.id.patientNumber);

        pSave = (Button) findViewById(R.id.save);
        pBack = (Button) findViewById(R.id.back);

        mAuth = FirebaseAuth.getInstance();
        patientID = mAuth.getCurrentUser().getUid();

        patientDatabase = FirebaseDatabase.getInstance().getReference().child("users").child("patient").child(patientID);

        getPatientData();

        pSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveData();
            }
        });

        pBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void getPatientData()
    {
        patientDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0)
                {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();

                    if(map.get("name") != null)
                    {
                        pNameStr = map.get("name").toString();
                        pName.setText(pNameStr);
                    }
                    if(map.get("number") != null)
                    {
                        pNumberStr = map.get("number").toString();
                        pNumber.setText(pNumberStr);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    private void saveData()
    {
        pNameStr = pName.getText().toString();
        pNumberStr = pNumber.getText().toString();

        Map newData = new HashMap();
        newData.put("name", pNameStr);
        newData.put("number", pNumberStr);

        patientDatabase.updateChildren(newData);

        finish();
    }

}
