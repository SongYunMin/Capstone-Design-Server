package com.example.capstone_design_qrserver;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;

import static com.example.capstone_design_qrserver.NFCActivity.status;

public class MainActivity extends AppCompatActivity {
    private IntentIntegrator qrScan;
    private BluetoothSPP bt;
    public static String Local_hash;
    public static String temp;
    public Button nfcButton;
    public Button QRButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        nfcButton = findViewById(R.id.NFCMode);
        QRButton = findViewById(R.id.QRMode);

        bt = new BluetoothSPP(this); //Initializing

        if (!bt.isBluetoothAvailable()) { //블루투스 사용 불가
            Toast.makeText(getApplicationContext()
                    , "Bluetooth is not available"
                    , Toast.LENGTH_SHORT).show();
            finish();
        }


        // QR Click Listener
        QRButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startQR();
            }
        });

        // NFC Click Listener
        nfcButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), NFCActivity.class);
                startActivityForResult(intent, 1);
            }
        });

        bt.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener() { //데이터 수신
            public void onDataReceived(byte[] data, String message) {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });

        //연결됐을 때
        bt.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() {
            public void onDeviceConnected(String name, String address) {
                Toast.makeText(getApplicationContext()
                        , "Connected to " + name + "\n" + address
                        , Toast.LENGTH_SHORT).show();
            }


            public void onDeviceDisconnected() { //연결해제
                Toast.makeText(getApplicationContext()
                        , "Connection lost", Toast.LENGTH_SHORT).show();
            }

            public void onDeviceConnectionFailed() { //연결실패
                Toast.makeText(getApplicationContext()
                        , "Unable to connect", Toast.LENGTH_SHORT).show();
            }
        });

        Button btnConnect = findViewById(R.id.btnConnect); //연결시도
        btnConnect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (bt.getServiceState() == BluetoothState.STATE_CONNECTED) {
                    bt.disconnect();
                } else {
                    Intent intent = new Intent(getApplicationContext(), DeviceList.class);
                    startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
                }
            }
        });

    }

    public void startQR() {
        qrScan = new IntentIntegrator(this);
        qrScan.setOrientationLocked(false);
        qrScan.setCameraId(1);                          // 전면카메라 사용
        qrScan.initiateScan();
    }

    public void onDestroy() {
        super.onDestroy();
        bt.stopService(); //블루투스 중지
    }

    public void onStart() {
        super.onStart();
        if (!bt.isBluetoothEnabled()) { //
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, BluetoothState.REQUEST_ENABLE_BT);
        } else {
            if (!bt.isServiceAvailable()) {
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_OTHER); //DEVICE_ANDROID는 안드로이드 기기 끼리
                setup();
            }
        }
    }

    public void setup() {
        Button btnSend = findViewById(R.id.btnSend); //데이터 전송
        btnSend.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                bt.send("Text", true);
            }
        });
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
            if (resultCode == Activity.RESULT_OK) {
                bt.connect(data);
            }
        } else if (requestCode == BluetoothState.REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_OTHER);
                setup();
            } else {
                Toast.makeText(getApplicationContext()
                        , "Bluetooth was not enabled."
                        , Toast.LENGTH_SHORT).show();
                finish();
            }
        } else if (result != null) {
            // 데이터가 없다면
            if (result.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            }
            // 데이터가 있다면
            else if (status == 0) {
                Local_hash = result.getContents();
                Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
                HttpConnectThread http = new HttpConnectThread(
                        "http://192.168.0.108/admission.php",
                        "localhash=" + Local_hash);
                http.start();
                for (int i = 0; i < 5000; i++) {
                    System.out.println("Test");
                }
                // 웹서버 결과값 받음
                temp = http.GetResult();
                // 예약이 되어있는 티켓이라면?
                if (temp.equals("true\n")) { // 티켓 일치시 구현부
                    Toast.makeText(this, "인증 되었습니다. 입장하십시오.",
                            Toast.LENGTH_SHORT).show();
                    bt.send("1", true);
                }

                // 예약이 되어있는 티켓이 아니라면?
                else {
                    Toast.makeText(this, "존재하지 않는 티켓입니다.",
                            Toast.LENGTH_SHORT).show();
                    bt.send("0", true);// 티켓 불일치시 구현부
                }

//                qrScan.initiateScan();              // 재귀적 구현 (Loop 위해)
            }
        } else if (requestCode == 1) {
            if (temp.equals("true\n")) { // 티켓 일치시 구현부
                Toast.makeText(this, "인증 되었습니다. 입장하십시오.",
                        Toast.LENGTH_SHORT).show();
                bt.send("1", true);
            }
            // 예약이 되어있는 티켓이 아니라면?
            else {
                Toast.makeText(this, "존재하지 않는 티켓입니다.",
                        Toast.LENGTH_SHORT).show();
                bt.send("0", true);// 티켓 불일치시 구현부
            }
        }
    }

}
