package com.jlink.settings.hidremote;

import android.annotation.SuppressLint;
import android.app.Instrumentation;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;

import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.util.UUID;
import com.jlink.settings.hidremote.HidMessageInfo;

public class HidReceiverService extends Service {
    private static final String TAG = HidReceiverService.class.getSimpleName();
    public int PORT = 8888;
    public final UUID MY_UUID = UUID.fromString("a0147dfb-08c0-41ce-baa5-3e10b692a340");

    public String connectedDeviceModel;
    public static volatile boolean isRunning;
    public static volatile boolean isBtOn;
    public static volatile boolean isHidUserOn = true;


    private NsdManager mNsdManager;
    private WifiManager wifiManager;
    private BluetoothManager bluetoothManager;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothServerSocket bluetoothServerSocket;
    private Context mContext = null;

    private Thread btReceiverThread;
    private Thread udpReceiverThread;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        isRunning = true;
        mNsdManager = (NsdManager) this.getSystemService(Context.NSD_SERVICE);
        wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(hidreceiver, filter);

        if (!wifiManager.isWifiEnabled()) {
            Log.d(TAG, "setWifiEnabled");
            wifiManager.setWifiEnabled(true);
        }
        if (!bluetoothAdapter.isEnabled()) {
            Log.d(TAG, "bluetoothAdapter.enable");
            bluetoothAdapter.enable();
        }

        startUdpReceiver();
        registerNsdService();
        isBtOn = bluetoothAdapter.isEnabled();
        startBluetoothHidReceiver();
    }

    @SuppressLint("MissingPermission")
    private void btAdvertiseService() {
        BluetoothLeAdvertiser advertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)  // 包含设备名
                .addServiceUuid(new ParcelUuid(MY_UUID))  // 服务UUID
                .build();
        Log.i(TAG, "startAdvertising Service UUID =" + MY_UUID.toString());

        advertiser.startAdvertising(settings, data, new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settings) {
                Log.i(TAG, "startAdvertising onStartSuccess");
            }

            @Override
            public void onStartFailure(int errorCode) {
                if (errorCode == ADVERTISE_FAILED_DATA_TOO_LARGE) {
                    Log.e(TAG, "startAdvertising onStartFailure ADVERTISE_FAILED_DATA_TOO_LARGE");
                }
            }
        });
    }

    private void registerNsdService() {
        Log.d(TAG, "registerNsdService");
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setServiceName("HidRemoteControlService");
        serviceInfo.setServiceType("_http._tcp.");
        serviceInfo.setAttribute("brand", Build.BRAND);
        serviceInfo.setAttribute("model", Build.MODEL);
        serviceInfo.setAttribute("Sn", Build.getSerial());
        serviceInfo.setAttribute("mac", getWlanMacAddress());

        int port = getAvailablePort();
        serviceInfo.setPort(port);
        serviceInfo.setAttribute("hidport", String.valueOf(PORT));
        Log.d(TAG, "registerNsdService serviceInfo =" + serviceInfo.toString());
        mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);
    }

    private NsdManager.RegistrationListener registrationListener = new NsdManager.RegistrationListener() {
        @Override
        public void onRegistrationFailed(NsdServiceInfo nsdServiceInfo, int i) {
            Log.d(TAG, "onRegistrationFailed  nsdServiceInfo =" + nsdServiceInfo.toString() + " ,i =" + i);
        }

        @Override
        public void onUnregistrationFailed(NsdServiceInfo nsdServiceInfo, int i) {
            Log.d(TAG, "onUnregistrationFailed  nsdServiceInfo =" + nsdServiceInfo.toString() + " ,i =" + i);
        }

        @Override
        public void onServiceRegistered(NsdServiceInfo nsdServiceInfo) {
            Log.d(TAG, "onServiceRegistered  nsdServiceInfo =" + nsdServiceInfo.toString());
        }

        @Override
        public void onServiceUnregistered(NsdServiceInfo nsdServiceInfo) {
            Log.d(TAG, "onServiceUnregistered  nsdServiceInfo =" + nsdServiceInfo.toString());
        }
    };


    private void startUdpReceiver() {
        udpReceiverThread = new Thread(() -> {
            try {
                PORT = getAvailablePort();
                Log.d(TAG, "UdpBroadcastReceiver run PORT " + PORT);
                DatagramSocket socket = new DatagramSocket(PORT);
                socket.setBroadcast(true);
                socket.setSoTimeout(3000);
                Log.d(TAG, "UdpBroadcastReceiver setBroadcast");
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                Gson gson = new Gson();
                HidMessageInfo info;
                while (isHidUserOn) {
                    try {
                        //Log.d(TAG, "UdpBroadcastReceiver receive poll");
                        socket.receive(packet);
                        Log.d(TAG, "UdpBroadcastReceiver receive end");
                        String message = new String(packet.getData(), 0, packet.getLength());
                        Log.d(TAG, "Received: (" + packet.getAddress().getHostAddress() + ":" + packet.getPort() + ") " + message);
                        info = gson.fromJson(message, HidMessageInfo.class);
                        Log.d(TAG, "Received: UdpMessageInfo " + info.keycode);
                        processMessages(info);
                    } catch (Exception e) {
                        if (e instanceof SocketTimeoutException) {
                        } else if (e instanceof JsonSyntaxException) {
                        } else {
                            Log.d(TAG, e.toString());
                        }
                    }
                }
                socket.close();
            } catch (Exception e) {
                Log.e(TAG, "Receive error: " + e.toString());
            }
        });
        udpReceiverThread.start();
    }

    private static int getAvailablePort() {
        try {
            ServerSocket serverSocket = new ServerSocket(0);
            int port = serverSocket.getLocalPort();
            serverSocket.close();
            return port;
        } catch (IOException e) {
            Log.d(TAG, e.toString());
            return -1;
        }
    }

    private void processMessages(HidMessageInfo info) {
        Log.d(TAG, "processMessages id " + info.getId());
        if (info.getType() == HidMessageInfo.TYPE_KEY) {
            processMessagesKey(info.getKeycode(), info.getKeycodeAct());
        } else if (info.getType() == HidMessageInfo.TYPE_ACTION) {
            processMessagesAction(info.getAction(), info.getPackageName(), info.getActName());
        } else if (info.getType() == HidMessageInfo.TYPE_CMD) {
            processMessagesCmd(info.getCmdline());
        } else if (info.getType() == HidMessageInfo.TYPE_INFO) {
            processMessagesInfo(info.getInfo());
        } else {
            Log.d(TAG, "UnKnow message type, ignore");
        }
    }

    private void processMessagesKey(int keycode, int keyAct) {
        Instrumentation inst = new Instrumentation();
        if (keyAct == KeyEvent.ACTION_UP) {
            Log.e(TAG, "sendKeySync ACTION_UP " + keycode);
            inst.sendKeySync(new KeyEvent(KeyEvent.ACTION_UP, keycode));
        } else if (keyAct == KeyEvent.ACTION_DOWN) {
            Log.e(TAG, "sendKeySync ACTION_DOWN " + keycode);
            inst.sendKeySync(new KeyEvent(KeyEvent.ACTION_DOWN, keycode));
        } else {
            Log.e(TAG, "sendKeyDownUpSync  " + keycode);
            inst.sendKeyDownUpSync(keycode);
        }
    }

    private void processMessagesAction(String action, String packagename, String actname) {
        Intent intent = new Intent();
        if (action != null) {
            intent.setAction(action);
        }
        if (packagename != null) {
            intent.setPackage(packagename);
            if (actname != null) {
                intent.setClassName(packagename, actname);
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    private void processMessagesCmd(String cmdline) {
        new Thread(() -> {
            String result = execCommand(cmdline);
            Log.e(TAG, "cmdline (" + cmdline + "): " + result);
        }).start();
    }

    private void processMessagesInfo(String cmdline) {
        connectedDeviceModel = cmdline;
        Log.e(TAG, "update connectedDeviceModel " + connectedDeviceModel);
    }

    public static String execCommand(String command) {
        StringBuilder output = new StringBuilder();
        try {
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            process.waitFor();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        return output.toString();
    }

    public static String getWlanMacAddress() {
        try {
            BufferedReader br = new BufferedReader(new FileReader("/sys/class/net/wlan0/address"));
            return br.readLine().trim().toUpperCase();
        } catch (IOException e) {
            return "02:00:00:00:00:00";
        }
    }

    @SuppressLint("MissingPermission")
    private void startBluetoothHidReceiver() {
        btReceiverThread = new Thread(() -> {
            Log.d(TAG, "startBluetoothHidReceiver");
            try {
                bluetoothServerSocket = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("HidAppService", MY_UUID);
            } catch (IOException e) {
                Log.d(TAG, "startBluetoothHidReceiver: " + e.toString());
                return;
            }

            btAdvertiseService();
            BluetoothSocket socket;
            byte[] buffer = new byte[1024];
            int bytes;
            InputStream tmpIn = null;
            while (isHidUserOn && isBtOn) {
                try {
                    Log.d(TAG, "bluetoothServerSocket.accept");
                    socket = bluetoothServerSocket.accept(); // 阻塞等待连接
                    tmpIn = socket.getInputStream();
                } catch (IOException e) {
                    Log.e(TAG, e.toString());
                    continue;
                }
                while (isHidUserOn && isBtOn) {
                    try {
                        bytes = tmpIn.read(buffer);
                        String receivedData = new String(buffer, 0, bytes);
                        Log.d(TAG, "btReceiverThread receivedData: " + receivedData);
                        Gson gson = new Gson();
                        HidMessageInfo info = gson.fromJson(receivedData, HidMessageInfo.class);
                        //Log.d(TAG, "Received: btMessageInfo " + info.keycode);
                        processMessages(info);
                        // 处理接收到的数据
                    } catch (Exception e) {
                        if (e instanceof JsonSyntaxException) {
                        } else {
                            Log.e(TAG, e.toString());
                            break;
                        }
                    }
                }
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        Log.e(TAG, e.toString());
                    }
                }
            }
            Log.d(TAG, "BluetoothHidReceiver finish");
        });
        btReceiverThread.start();
    }

    private final BroadcastReceiver hidreceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(TAG, "onReceive: " + intent.getAction());
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_ON) {
                    isBtOn = bluetoothAdapter.isEnabled();
                    Log.d(TAG, "onReceive BluetoothAdapter.STATE_ON " + isBtOn);
                    if (isBtOn && (!btReceiverThread.isAlive())) {
                        Log.d(TAG, "onReceive btReceiverThread.start()");
                        startBluetoothHidReceiver();
                    }
                } else if (state == BluetoothAdapter.STATE_OFF) {
                    isBtOn = bluetoothAdapter.isEnabled();
                    Log.d(TAG, "onReceive BluetoothAdapter.STATE_OFF " + isBtOn);
                }
            } else if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
                if (state == WifiManager.WIFI_STATE_ENABLED) {
                    Log.d(TAG, "onReceive WifiManager.WIFI_STATE_ENABLED ");
                } else if (state == WifiManager.WIFI_STATE_DISABLED) {
                    Log.d(TAG, "onReceive WifiManager.WIFI_STATE_DISABLED ");
                }
            }

        }
    };


}
