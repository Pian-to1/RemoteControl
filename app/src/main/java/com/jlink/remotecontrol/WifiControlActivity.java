package com.jlink.remotecontrol;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

public class WifiControlActivity extends AppCompatActivity {

    private static final String TAG = "RemoteControl -- " + WifiControlActivity.class.getSimpleName();
    private UdpBroadcastSender udpBroadcastSender;
    private int index = 0;
    private String sn,mac,model;

    private RelativeLayout number_view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.remote_control);

        udpBroadcastSender = new UdpBroadcastSender();

        udpBroadcastSender.PORT  = getIntent().getIntExtra("port", 8888);
        udpBroadcastSender.IP = getIntent().getStringExtra("ip");

        sn = getIntent().getStringExtra("sn");
        mac = getIntent().getStringExtra("mac");
        model = getIntent().getStringExtra("name");

        initView();
    }

    @SuppressLint("SetTextI18n")
    private void initView() {
        TextView bt_status = findViewById(R.id.bt_status);
        bt_status.setText(mac);
        bt_status.setTextColor(Color.GREEN);

        RelativeLayout info = findViewById(R.id.info);
        TextView device_info = findViewById(R.id.device_info);
        device_info.setText("SN : " + sn + "\nmodel : " + model + "\nip : "
                + udpBroadcastSender.IP + "\nport : " + udpBroadcastSender.PORT);

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
    }

    public void sendUdpMessage(String msg) {

        Log.d(TAG, "sendUdpMessage *** msg : " + msg);
        new Thread(() -> {
            udpBroadcastSender.sendBroadcast(msg, udpBroadcastSender.IP, udpBroadcastSender.PORT);
        }).start();
    }

    private void sendKeyCommand(int keyCode) {
        Log.d(TAG, "sendKeyCommand TYPE_KEY === : keyCode = " + keyCode);
        HidMessageInfo udpMessageInfo = new HidMessageInfo();
        udpMessageInfo.setId(++index);
        udpMessageInfo.setType(HidMessageInfo.TYPE_KEY);
        udpMessageInfo.setKeycode(keyCode);
        Gson gson = new Gson();
        String json = gson.toJson(udpMessageInfo);
        sendUdpMessage(json);
    }

    private void sendActionCommand(String action) {
        Log.d(TAG, "sendActionCommand TYPE_ACTION === : action = " + action);
        HidMessageInfo udpMessageInfo = new HidMessageInfo();
        udpMessageInfo.setId(++index);
        udpMessageInfo.setType(HidMessageInfo.TYPE_ACTION);
        udpMessageInfo.setAction(action);
        Gson gson = new Gson();
        String json = gson.toJson(udpMessageInfo);
        sendUdpMessage(json);
    }

    private void sendInfoCommand(String info) {
        Log.d(TAG, "sendInfoCommand TYPE_INFO === : info = " + info);
        HidMessageInfo udpMessageInfo = new HidMessageInfo();
        udpMessageInfo.setId(++index);
        udpMessageInfo.setType(HidMessageInfo.TYPE_INFO);
        udpMessageInfo.setInfo(info);
        Gson gson = new Gson();
        String json = gson.toJson(udpMessageInfo);
        sendUdpMessage(json);
    }
}