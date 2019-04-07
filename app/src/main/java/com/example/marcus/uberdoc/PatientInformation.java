package com.example.marcus.uberdoc;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class PatientInformation extends AppCompatActivity {


    private TextView pName, pNumber;
    private EditText pCondition, pAllergy, pMedication;
    private Button pSave, pBack;


    private DatabaseReference patientDatabase;
    private String patientID = "", pNameStr, pNumberStr, pConditionStr, pAllergyStr, pMedicationStr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_information);

//        Intent intent = getIntent();
//        patientID = intent.getStringExtra(patientID.toString());

        pName = (TextView) findViewById(R.id.patientName);
        pNumber = (TextView) findViewById(R.id.patientNumber);
        pCondition = (EditText) findViewById(R.id.patientConditions);
        pAllergy = (EditText) findViewById(R.id.patientAllergies);
        pMedication = (EditText) findViewById(R.id.patientMedications);

        pSave = (Button) findViewById(R.id.save);
        pBack = (Button) findViewById(R.id.back);


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
        getAssignedPatient();

    }


    private void getAssignedPatient(){
        String doctorID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedPatientRef  = FirebaseDatabase.getInstance().getReference().child("users").child("doctor").child(doctorID).child("nextPatient");

        assignedPatientRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists())
                {
                    patientID = dataSnapshot.getValue().toString();

                    getPatientData();

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void getPatientData()
    {
        patientDatabase = FirebaseDatabase.getInstance().getReference().child("users").child("patient").child(patientID);

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
                    if(map.get("conditions") != null)
                    {
                        pConditionStr = map.get("conditions").toString();
                        pCondition.setText(pConditionStr);
                    }
                    if(map.get("allergies") != null)
                    {
                        pAllergyStr = map.get("allergies").toString();
                        pAllergy.setText(pAllergyStr);
                    }
                    if(map.get("medications") != null)
                    {
                        pMedicationStr = map.get("medications").toString();
                        pMedication.setText(pMedicationStr);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    private void saveData()
    {
        pConditionStr = pCondition.getText().toString();
        pAllergyStr = pAllergy.getText().toString();
        pMedicationStr = pMedication.getText().toString();

        Map newData = new HashMap();
        newData.put("conditions", pConditionStr);
        newData.put("allergies", pAllergyStr);
        newData.put("medications", pMedicationStr);

        patientDatabase.updateChildren(newData);

        finish();
    }
}
