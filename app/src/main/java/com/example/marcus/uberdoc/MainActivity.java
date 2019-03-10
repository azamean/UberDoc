package com.example.marcus.uberdoc;

import android.support.v7.app.AppCompatActivity;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;


public class MainActivity extends AppCompatActivity {


    Button doctorbtn, patientbtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        doctorbtn = (Button) findViewById(R.id.doctorbtn);
        patientbtn = (Button) findViewById(R.id.patientbtn);

        doctorbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, doctorLogin.class);
                startActivity(intent);
            }
        });
        patientbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, patientLogin.class);
                startActivity(intent);
            }
        });

    }
}
