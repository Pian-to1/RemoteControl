package com.jlink.remotecontrol;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class BTDeviceAdapter extends RecyclerView.Adapter<DeviceItem> {

    private static final String TAG = "RemoteControl -- " + BTDeviceAdapter.class.getSimpleName();
    private final Context mContext;
    private final List<BluetoothDevice> deviceList;

    public BTDeviceAdapter(Context mContext, List<BluetoothDevice> list) {
        this.mContext = mContext;
        this.deviceList = list;
    }

    public void addDevice(BluetoothDevice newDevice) {
        deviceList.add(newDevice);
        notifyItemInserted(deviceList.size() - 1);
    }

    @SuppressLint({"UseCompatLoadingForDrawables", "ResourceType"})
    @Override
    public void onBindViewHolder(DeviceItem holder, int position) {
        BluetoothDevice device = deviceList.get(position);

        holder.itemView.setOnClickListener(v -> {
            Intent bt = new Intent(mContext, BTControlActivity.class);
            bt.putExtra("connectedDeviceAddress", device.getAddress());
            bt.putExtra("connectedDeviceName", device.getName());
            mContext.startActivity(bt);
        });

        holder.iv_device.setImageDrawable(mContext.getDrawable(R.drawable.ic_projector));
        holder.device_name.setText(device.getName());
        holder.device_type.setText("投影仪");
        holder.device_mac.setText(device.getAddress());
//        holder.device_sn.setText("sn");
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
