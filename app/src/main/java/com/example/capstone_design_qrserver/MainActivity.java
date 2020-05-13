package com.example.capstone_design_qrserver;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;

public class MainActivity extends AppCompatActivity {
    private IntentIntegrator qrScan;
    public String Local_hash;
    private BluetoothSPP bt = new BluetoothSPP(this);
    int status = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        qrScan = new IntentIntegrator(this);
        qrScan.setCameraId(1);                          // 전면카메라 사용
        qrScan.setOrientationLocked(false);
        qrScan.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            // 데이터가 없다면
            if (result.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
                // todo
            }
            // 데이터가 있다면
            else {
                Local_hash = result.getContents();
                Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
                HttpConnectThread http = new HttpConnectThread(
                        "http://210.124.110.96/admission.php",
                        "localhash=" + Local_hash);
                http.start();
                // 웹서버 결과값 받음
                String temp = http.GetResult();
                // 예약이 되어있는 티켓이라면?
                if (temp.equals("true\n")) { // 티켓 일치시 구현부
                    status = 1;
                }
                // 예약이 되어있는 티켓이 아니라면?
                else {                               // 티켓 불일치시 구현부

                }
                qrScan.initiateScan();              // 재귀적 구현 (Loop 위해)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

}


