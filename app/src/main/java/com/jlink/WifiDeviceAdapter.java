package com.jlink.remotecontrol;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class WifiDeviceAdapter extends RecyclerView.Adapter<DeviceItem> {

    private static final String TAG = "RemoteControl -- " + WifiDeviceAdapter.class.getSimpleName();
    private final Context mContext;
    private final List<NsdServiceInfo> deviceList;

    public WifiDeviceAdapter(Context mContext, List<NsdServiceInfo> list) {
        this.mContext = mContext;
        this.deviceList = list;
    }

    public void addDevice(NsdServiceInfo newDevice) {
        deviceList.add(newDevice);
        notifyItemInserted(deviceList.size() - 1);
    }

    @SuppressLint({"UseCompatLoadingForDrawables", "ResourceType"})
    @Override
    public void onBindViewHolder(DeviceItem holder, int position) {
        NsdServiceInfo device = deviceList.get(position);

        String ip = device.getHost().toString().substring(1);
        Map<String, byte[]> attributes = device.getAttributes();
        String HidPort = new String(attributes.get("hidport"), StandardCharsets.UTF_8);
        String sn = new String(attributes.get("Sn"), StandardCharsets.UTF_8);
        String mac = new String(attributes.get("mac"), StandardCharsets.UTF_8);
        String model = new String(attributes.get("model"), StandardCharsets.UTF_8);
        int port = Integer.parseInt(HidPort);

        Log.d(TAG, "onBindViewHolder : ip = " + ip + ", port = " + port + ", sn = " + sn
                + ", mac = " + mac + ", name = " + model);

        holder.itemView.setOnClickListener(v -> {
            Intent wifi = new Intent(mContext, WifiControlActivity.class);
            wifi.putExtra("ip", ip);
            wifi.putExtra("port", port);
            wifi.putExtra("sn", sn);
            wifi.putExtra("mac", mac);
            wifi.putExtra("name", model);
            mContext.startActivity(wifi);
        });

        holder.iv_device.setImageDrawable(mContext.getDrawable(R.drawable.ic_projector));
        holder.device_name.setText(model);
        holder.device_type.setText("投影仪");
        holder.device_mac.setText(sn);
    }

    @NonNull
    @Override
    public DeviceItem onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.new_device, parent, false);
        return new DeviceItem(view);
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

}
