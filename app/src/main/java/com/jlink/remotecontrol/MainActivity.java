package com.jlink.remotecontrol;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "RemoteControl -- " + MainActivity.class.getSimpleName();
    public final UUID MY_UUID = UUID.fromString("a0147dfb-08c0-41ce-baa5-3e10b692a340");
    private BluetoothAdapter mBtAdapter;

    private List<BluetoothDevice> btList;
    private BTDeviceAdapter btDeviceAdapter;

    private NsdManager mNsdManager;
    private UdpBroadcastSender udpBroadcastSender;

    private List<NsdServiceInfo> wifiList;
    private WifiDeviceAdapter wifiDeviceAdapter;


    private RecyclerView bt_view, wifi_view;
    private Context mContext;

    @SuppressLint({"MissingInflatedId", "HardwareIds"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;
        bt_view = findViewById(R.id.bt_list);
        wifi_view = findViewById(R.id.wifi_list);

        //蓝牙列表
        btList = new ArrayList<>();
        btDeviceAdapter = new BTDeviceAdapter(mContext, btList);
        bt_view.setLayoutManager(new LinearLayoutManager(mContext));
        bt_view.setAdapter(btDeviceAdapter);

        BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBtAdapter = bluetoothManager.getAdapter();
        BluetoothLeScanner scanner = mBtAdapter.getBluetoothLeScanner();
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        List<ScanFilter> filters = new ArrayList<>();
        filters.add(new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(MY_UUID))
                .build());

        //wifi列表
        wifiList = new ArrayList<>();
        wifiDeviceAdapter = new WifiDeviceAdapter(mContext, wifiList);
        wifi_view.setLayoutManager(new LinearLayoutManager(mContext));
        wifi_view.setAdapter(wifiDeviceAdapter);

        udpBroadcastSender = new UdpBroadcastSender();
        mNsdManager = (NsdManager) mContext.getSystemService(Context.NSD_SERVICE);

        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
            };
            if (ContextCompat.checkSelfPermission(mContext, permissions[0]) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions, 100);
                return;
            }
        }

        Log.d(TAG, "startScan-----");
        scanner.startScan(filters, settings, scanCallback);

        Log.d(TAG, "discoverServices-----");
        mNsdManager.discoverServices("_http._tcp.", NsdManager.PROTOCOL_DNS_SD, discoveryListener);
    }

    private final NsdManager.DiscoveryListener discoveryListener = new NsdManager.DiscoveryListener() {
        @Override
        public void onStartDiscoveryFailed(String s, int i) {
            Log.d(TAG, "onStartDiscoveryFailed " + i);
            Log.d(TAG, "onStartDiscoveryFailed " + s);
        }

        @Override
        public void onStopDiscoveryFailed(String s, int i) {
            Log.d(TAG, "onStopDiscoveryFailed " + i);
            Log.d(TAG, "onStopDiscoveryFailed " + s);
        }

        @Override
        public void onDiscoveryStarted(String s) {
            Log.d(TAG, "onDiscoveryStarted " + s);
        }

        @Override
        public void onDiscoveryStopped(String s) {
            Log.d(TAG, "onDiscoveryStopped " + s);
        }

        @Override
        public void onServiceFound(NsdServiceInfo nsdServiceInfo) {
            Log.d(TAG, "onServiceFound " + nsdServiceInfo.toString());
            mNsdManager.resolveService(nsdServiceInfo, resolveListener);
        }

        @Override
        public void onServiceLost(NsdServiceInfo nsdServiceInfo) {
            Log.d(TAG, "onServiceLost " + nsdServiceInfo.toString());
        }
    };

    private final NsdManager.ResolveListener resolveListener = new NsdManager.ResolveListener() {
        @Override
        public void onResolveFailed(NsdServiceInfo nsdServiceInfo, int i) {
            Log.d(TAG, "onResolveFailed " + i);
            Log.d(TAG, "onResolveFailed " + nsdServiceInfo.toString());
        }

        @Override
        public void onServiceResolved(NsdServiceInfo nsdServiceInfo) {
            Log.d(TAG, "onServiceResolved " + nsdServiceInfo.toString());

            if (!wifiList.contains(nsdServiceInfo)) {
                MainActivity.this.runOnUiThread(() -> wifiDeviceAdapter.addDevice(nsdServiceInfo));
            }
            Map<String, byte[]> attributes = nsdServiceInfo.getAttributes();
            String nsdHost = nsdServiceInfo.getHost().toString();
            udpBroadcastSender.IP = nsdHost.substring(1);
            String HidPort = new String(attributes.get("hidport"), StandardCharsets.UTF_8);
            udpBroadcastSender.PORT = Integer.parseInt(HidPort);

            Log.d(TAG, "onServiceResolved udpBroadcastSender.IP =" + udpBroadcastSender.IP);
            Log.d(TAG, "onServiceResolved udpBroadcastSender.PORT =" + udpBroadcastSender.PORT);
        }
    };

    private final ScanCallback scanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            Log.d(TAG, "Found device: " + device +"("+device.getName()+"), UUID: " + result.getScanRecord().getServiceUuids());
            if (!btList.contains(device)) {
                Log.d(TAG, "addDevice === " +" *** "+device.getName());
                btDeviceAdapter.addDevice(device);
            }
        }
    };

}
