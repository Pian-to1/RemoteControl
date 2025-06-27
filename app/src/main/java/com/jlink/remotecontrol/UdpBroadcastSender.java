package com.jlink.remotecontrol;


import android.util.Log;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UdpBroadcastSender {
    private static final String TAG = "UdpBroadcastSender";
    public  int PORT = 8888;
    public  String IP = "255.255.255.255";

    public static void sendBroadcast(String message,String Ip, int port) {
        try {
            DatagramSocket socket = new DatagramSocket();
            socket.setBroadcast(true);

            byte[] buffer = message.getBytes();
            InetAddress address = InetAddress.getByName(Ip);

            DatagramPacket packet = new DatagramPacket(
                    buffer, buffer.length, address, port);
            socket.send(packet);
            socket.close();

            Log.d(TAG, "Broadcast sent: " + message);
        } catch (Exception e) {
            Log.e(TAG, "Broadcast error: " + e.toString());
        }
    }
}

