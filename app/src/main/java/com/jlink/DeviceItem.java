package com.jlink.remotecontrol;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class DeviceItem extends RecyclerView.ViewHolder {
    public ImageView iv_device;
    public TextView device_name;
    public TextView device_mac;
    public TextView device_sn;
    public TextView device_type;

    public DeviceItem(@NonNull View itemView) {
        super(itemView);
        iv_device = itemView.findViewById(R.id.iv_device);
        device_name = itemView.findViewById(R.id.device_name);
        device_mac = itemView.findViewById(R.id.device_mac);
//        device_sn = itemView.findViewById(R.id.device_sn);
        device_type = itemView.findViewById(R.id.device_type);

    }

    public ImageView getIvDevice() {
        return iv_device;
    }

    public void setIvDevice(ImageView imageView) {
        this.iv_device = imageView;
    }

    public TextView getName() {
        return device_name;
    }

    public void setName(TextView textView) {
        this.device_name = textView;
    }

    public TextView getMac() {
        return device_mac;
    }

    public void setMac(TextView textView) {
        this.device_mac = textView;
    }

    public TextView getSN() {
        return device_sn;
    }

    public void setSN(TextView textView) {
        this.device_sn = textView;
    }

    public TextView getType() {
        return device_type;
    }

    public void setType(TextView textView) {
        this.device_type = textView;
    }
}