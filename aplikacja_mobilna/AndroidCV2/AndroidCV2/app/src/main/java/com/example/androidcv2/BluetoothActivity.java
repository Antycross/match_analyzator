package com.example.androidcv2;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

import static com.example.androidcv2.MainActivity.setAdapter;
import static com.example.androidcv2.MainActivity.setBluetoothDevice;

public class BluetoothActivity extends AppCompatActivity {

    ListView listView;
    Button scan;
    BluetoothAdapter adapter;
    ArrayList<BluetoothDevice> BDArrayList = new ArrayList<BluetoothDevice>();
    ArrayList<String> stringArrayList = new ArrayList<String>();
    ArrayAdapter<String> arrayAdapter;
    Boolean success = false;
    private static final int REQUEST_ENABLE_BT = 2;
    public static Activity BA;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        BA = this;
        listView = findViewById(R.id.list_view);
        scan = findViewById(R.id.scan_button);
        adapter = BluetoothAdapter.getDefaultAdapter();

        checkPermissions();
        enableBluetooth();

        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(adapter != null){
                    if(adapter.isDiscovering()){
                        adapter.cancelDiscovery();
                    }
                    success = adapter.startDiscovery();
                    if(success){
                        Toast.makeText(getApplicationContext(), "Searching...", Toast.LENGTH_LONG).show();
                    }
                    else{
                        Toast.makeText(getApplicationContext(), "Searching error", Toast.LENGTH_LONG).show();
                    }

                }
            }
        });

        listViewEnable();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                adapter.cancelDiscovery();
                String deviceName = BDArrayList.get(position).getName();
                Boolean connected = BDArrayList.get(position).createBond();
                if(connected){
                    Toast.makeText(getApplicationContext(), "You are bond with " + deviceName, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    setAdapter(adapter);
                    setBluetoothDevice(BDArrayList.get(position));
                    intent.putExtra("CONNECTED", connected);
                    startActivity(intent);
                }
                else{
                    Toast.makeText(getApplicationContext(), "Error with bounding" + deviceName, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void enableBluetooth() {
        if(adapter == null) {
            Toast.makeText(getApplicationContext(), "Your device not support Bluetooth communication", Toast.LENGTH_LONG).show();
            success = false;
        }
        else {
            if (!adapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    private void checkPermissions() {
        if(ActivityCompat.checkSelfPermission(BluetoothActivity.this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(BluetoothActivity.this, new String[]{Manifest.permission.BLUETOOTH}, 1);
            return;
        }
        if(ActivityCompat.checkSelfPermission(BluetoothActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(BluetoothActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        if(ActivityCompat.checkSelfPermission(BluetoothActivity.this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(BluetoothActivity.this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, 1);
            return;
        }
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                BDArrayList.add(device);
                stringArrayList.add(device.getName());
                arrayAdapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK){
            Toast.makeText(getApplicationContext(), "Bluetooth is now enabled", Toast.LENGTH_SHORT).show();
        }
    }

    private void listViewEnable() {
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, intentFilter);
        arrayAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, stringArrayList);
        listView.setAdapter(arrayAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }
}







