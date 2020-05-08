package com.example.capstone_design_qrserver;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.google.zxing.integration.android.IntentIntegrator;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new IntentIntegrator(this).initiateScan();



    }
}
