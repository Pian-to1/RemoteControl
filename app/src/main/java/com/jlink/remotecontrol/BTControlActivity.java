package com.jlink.remotecontrol;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class BTControlActivity extends AppCompatActivity {

    private static final String TAG = "RemoteControl -- " + BTControlActivity.class.getSimpleName();

    public final UUID MY_UUID = UUID.fromString("a0147dfb-08c0-41ce-baa5-3e10b692a340");
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private String connectedDeviceAddress, connectedDeviceName;
    private BluetoothDevice connectedDevice;
    private boolean isRfcommConnecting = false;
    private int index = 0;

    private RelativeLayout number_view;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.remote_control);

        connectedDeviceAddress = getIntent().getStringExtra("connectedDeviceAddress");
        connectedDeviceName = getIntent().getStringExtra("connectedDeviceName");
        Log.d(TAG, "connectedDeviceAddress = " + connectedDeviceAddress
                + ", connectedDeviceName = " + connectedDeviceName);

        initView();

    }

    @SuppressLint("SetTextI18n")
    private void initView() {
        TextView bt_status = findViewById(R.id.bt_status);
        if (TextUtils.isEmpty(connectedDeviceAddress)) {
            bt_status.setText("未连接");
        } else {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                return;
            }

            connectedDevice = bluetoothAdapter.getRemoteDevice(connectedDeviceAddress);
            bt_status.setText(connectedDeviceAddress);
            bt_status.setTextColor(Color.GREEN);
            connectToDevice(connectedDevice);
        }

        RelativeLayout info = findViewById(R.id.info);
        TextView device_info = findViewById(R.id.device_info);

        Log.d(TAG, "connectedDevice.getUuids() = " + connectedDevice.getUuids());
        device_info.setText("model : " + connectedDevice.getName()
                + "\nuuid : " + MY_UUID.toString());

        bt_status.setOnClickListener(v -> {
            if (info.getVisibility() == View.GONE) {
                info.setVisibility(View.VISIBLE);
            } else {
                info.setVisibility(View.GONE);
            }
        });

        number_view = findViewById(R.id.number_view);

        findViewById(R.id.home_icon).setOnClickListener(v -> sendKeyCommand(KeyEvent.KEYCODE_HOME));
        findViewById(R.id.setting_icon).setOnClickListener(v -> sendActionCommand("android.settings.SETTINGS"));
        findViewById(R.id.power_icon).setOnClickListener(v -> sendKeyCommand(KeyEvent.KEYCODE_POWER));
        findViewById(R.id.up_icon).setOnClickListener(v -> sendKeyCommand(KeyEvent.KEYCODE_DPAD_UP));
        findViewById(R.id.down_icon).setOnClickListener(v -> sendKeyCommand(KeyEvent.KEYCODE_DPAD_DOWN));
        findViewById(R.id.left_icon).setOnClickListener(v -> sendKeyCommand(KeyEvent.KEYCODE_DPAD_LEFT));
        findViewById(R.id.right_icon).setOnClickListener(v -> sendKeyCommand(KeyEvent.KEYCODE_DPAD_RIGHT));
        findViewById(R.id.ok_icon).setOnClickListener(v -> sendKeyCommand(KeyEvent.KEYCODE_ENTER));
        findViewById(R.id.back_icon).setOnClickListener(v -> sendKeyCommand(KeyEvent.KEYCODE_BACK));
        findViewById(R.id.voice_icon).setOnClickListener(v -> sendKeyCommand(KeyEvent.KEYCODE_VOICE_ASSIST));
        findViewById(R.id.volume_up).setOnClickListener(v -> sendKeyCommand(KeyEvent.KEYCODE_VOLUME_UP));
        findViewById(R.id.volume_down).setOnClickListener(v -> sendKeyCommand(KeyEvent.KEYCODE_VOLUME_DOWN));
        findViewById(R.id.number_icon).setOnClickListener(v -> {
            if (number_view.getVisibility() == View.GONE) {
                number_view.setVisibility(View.VISIBLE);
            } else {
                number_view.setVisibility(View.GONE);
            }
        });

        findViewById(R.id.num_clear).setOnClickListener(v -> sendKeyCommand(KeyEvent.KEYCODE_PERIOD));
        findViewById(R.id.num_delete).setOnClickListener(v -> sendKeyCommand(KeyEvent.KEYCODE_DEL));
        findViewById(R.id.num_0).setOnClickListener(v -> sendKeyCommand(KeyEvent.KEYCODE_0));
        findViewById(R.id.num_1).setOnClickListener(v -> sendKeyCommand(KeyEvent.KEYCODE_1));
        findViewById(R.id.num_2).setOnClickListener(v -> sendKeyCommand(KeyEvent.KEYCODE_2));
        findViewById(R.id.num_3).setOnClickListener(v -> sendKeyCommand(KeyEvent.KEYCODE_3));
        findViewById(R.id.num_4).setOnClickListener(v -> sendKeyCommand(KeyEvent.KEYCODE_4));
        findViewById(R.id.num_5).setOnClickListener(v -> sendKeyCommand(KeyEvent.KEYCODE_5));
        findViewById(R.id.num_6).setOnClickListener(v -> sendKeyCommand(KeyEvent.KEYCODE_6));
        findViewById(R.id.num_7).setOnClickListener(v -> sendKeyCommand(KeyEvent.KEYCODE_7));
        findViewById(R.id.num_8).setOnClickListener(v -> sendKeyCommand(KeyEvent.KEYCODE_8));
        findViewById(R.id.num_9).setOnClickListener(v -> sendKeyCommand(KeyEvent.KEYCODE_9));

        findViewById(R.id.num_ok).setOnClickListener(v -> {
            EditText edit_input = findViewById(R.id.edit_input);
            String txt = edit_input.getText().toString();
            if (!TextUtils.isEmpty(txt)) {
                sendInfoCommand(txt);
                edit_input.setText("");
            }
        });

    }

    private void connectToDevice(BluetoothDevice device) {
        new Thread(() -> {
            Log.d(TAG, "connectToDevice : ");
            try {
                if(bluetoothSocket == null || !bluetoothSocket.getRemoteDevice().equals(device)) {
                    Log.d(TAG, "connectToDevice : createInsecureRfcommSocketToServiceRecord");
                    bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
                }
                if(!bluetoothSocket.isConnected()) {
                    Log.d(TAG, "connectToDevice : connect");
                    bluetoothSocket.connect();
                    isRfcommConnecting = true;

                }
                // 获取输出流
                outputStream = bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "连接失败", e);
                isRfcommConnecting = false;
            }

        }).start();
    }

    private void closeConnection() {
        try {
            if (outputStream != null) outputStream.close();
            if (bluetoothSocket != null) bluetoothSocket.close();
            Log.d(TAG, "关闭连接");

        } catch (IOException e) {
            Log.e(TAG, "关闭连接失败", e);
        }

        outputStream = null;
        bluetoothSocket = null;

    }

    private void sendKeyCommand(int keyCode) {
        Log.d(TAG, "sendKeyCommand TYPE_KEY === : keyCode = " + keyCode);
        HidMessageInfo udpMessageInfo = new HidMessageInfo();
        udpMessageInfo.setId(++index);
        udpMessageInfo.setType(HidMessageInfo.TYPE_KEY);
        udpMessageInfo.setKeycode(keyCode);
        Gson gson = new Gson();
        String json = gson.toJson(udpMessageInfo);
        sendBtRfcommMessage(json);
    }

    private void sendActionCommand(String action) {
        Log.d(TAG, "sendActionCommand TYPE_ACTION === : action = " + action);
        HidMessageInfo udpMessageInfo = new HidMessageInfo();
        udpMessageInfo.setId(++index);
        udpMessageInfo.setType(HidMessageInfo.TYPE_ACTION);
        udpMessageInfo.setAction(action);
        Gson gson = new Gson();
        String json = gson.toJson(udpMessageInfo);
        sendBtRfcommMessage(json);
    }


    private void sendInfoCommand(String info) {
        Log.d(TAG, "sendInfoCommand TYPE_INFO === : info = " + info);
        HidMessageInfo udpMessageInfo = new HidMessageInfo();
        udpMessageInfo.setId(++index);
        udpMessageInfo.setType(HidMessageInfo.TYPE_INFO);
        udpMessageInfo.setInfo(info);
        Gson gson = new Gson();
        String json = gson.toJson(udpMessageInfo);
        sendBtRfcommMessage(json);
    }

    public void sendBtRfcommMessage(String command) {

        if (isRfcommConnecting) {
            Log.d(TAG, "sendBtRfcommMessage : isRfcommConnecting " + isRfcommConnecting);
        } else {
            connectToDevice(connectedDevice);
        }

        if (outputStream == null) {
            Toast.makeText(this, "未连接到设备", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "未连接到设备");
            return;
        }

        new Thread(() -> {
            try {
                // 发送命令
                outputStream.write(command.getBytes());
                outputStream.flush();

            } catch (IOException e) {
                Log.d(TAG, e.toString());
                try {
                    if (bluetoothSocket != null) bluetoothSocket.close();
                    bluetoothSocket = null;
                    isRfcommConnecting = false;
                } catch (IOException ex) {
                    Log.d(TAG, e.toString());
                }
            }

        }).start();

    }

}