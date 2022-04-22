package com.example.clientbluetoothchat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    // MAC de HC-05 00:21:13:01:33:F9
    private final String DEVICE_ADDRESS = "00:21:13:01:33:F9";
    private final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    TextView textView;
    InputStream inputStream;
    BluetoothSocket socket;
    boolean stopThread;
    byte[] buffer;
    private BluetoothDevice device;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textView);
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }
        if (!bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.isDiscovering();
        }
        if (BTinit()) {
            if (BTconnect()) {
                beginListenForData();
            }
        }
    }

    public boolean BTinit() {
        boolean found = false;
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Appareil non compatible Bluetooth", Toast.LENGTH_SHORT).show();
        }
        assert bluetoothAdapter != null;
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableAdapter, 0);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        if (bondedDevices.isEmpty()) {
            Toast.makeText(getApplicationContext(), "S'il vous plaît, connectez votre appareil Bluetooth", Toast.LENGTH_SHORT).show();
        } else {
            for (BluetoothDevice iterator : bondedDevices) {
                if (iterator.getAddress().equals(DEVICE_ADDRESS)) {
                    device = iterator;
                    Log.d("Device", device.getName());
                    Log.d("Device", device.getAddress());
                    found = true;
                    break;
                }
            }
        }
        return found;
    }

    public boolean BTconnect() {
        boolean connected = true;
        try {
            socket = device.createRfcommSocketToServiceRecord(PORT_UUID);
            socket.connect();
            inputStream = socket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
            connected = false;
        }
        return connected;
    }

    void beginListenForData() {
        final Handler handler = new Handler();
        stopThread = false;
        buffer = new byte[1024];
        Thread thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted() && !stopThread) {
                try {
                    int byteCount = inputStream.available();
                    if (byteCount > 0) {
                        byte[] rawBytes = new byte[byteCount];
                        inputStream.read(rawBytes);
                        final String string = new String(rawBytes, StandardCharsets.UTF_8);
                        handler.post(() -> {
                            //données reçues : String
                            Log.d("DATA", string);
                            textView.append(string);
                        });
                    }
                } catch (IOException ex) {
                    stopThread = true;
                }
            }
        });
        thread.start();
    }
}