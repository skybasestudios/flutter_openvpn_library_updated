package de.blinkt.openvpn;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.OpenVPNThread;

public class OboloiVPN extends Activity {
    private static Activity activity;
    private static OnVPNStatusChangeListener listener;
    private static OpenVPNThread vpnThread = new OpenVPNThread();
    private static OpenVPNService vpnService = new OpenVPNService();
    private static String ovpnFileContent;
    private static String expireAt;
    private static boolean vpnStart = false;
    private static Intent profileIntent;

    public OboloiVPN(Activity activity) {
        OboloiVPN.activity = activity;
        LocalBroadcastManager.getInstance(activity).registerReceiver(broadcastReceiver, new IntentFilter("connectionState"));
    }

    public OboloiVPN(){

    }

    public void setOnVPNStatusChangeListener(OnVPNStatusChangeListener listener) {
        OboloiVPN.listener = listener;
    }

    public void launchVPN(String ovpnFileContent,String expireAt){
        OboloiVPN.ovpnFileContent = ovpnFileContent;
        OboloiVPN.expireAt = expireAt;
        OpenVPNService.expireAt = expireAt;
        profileIntent = VpnService.prepare(activity);
        if(profileIntent != null) {
            activity.startActivity(new Intent(activity, OboloiVPN.class));
            return;
        }
        if(listener != null) listener.onProfileLoaded(true);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.launchvpn);
        launchVPN();
    }

    private void launchVPN() {
        if (!vpnStart) {
                    startActivityForResult(profileIntent, 1);
                    //connecting status
        }
    }

    public void init(){
        if(vpnStart){
            stopVpn();
        }else{
            startVpn();
        }
    }

    private boolean stopVpn() {
        try {
            vpnThread.stop();
            //disconnected status
            vpnStart = false;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private void startVpn() {
        try {

            OpenVpnApi.startVpn(activity, ovpnFileContent, "Canada", "vpn", "vpn");

            //connecting status
            vpnStart = true;

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public String getServiceStatus() {
       return vpnService.getStatus();
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(activity == null  || activity.isDestroyed() || activity.isFinishing()){
                listener = null;
                if( activity != null)
                LocalBroadcastManager.getInstance(activity).unregisterReceiver(broadcastReceiver);
            }
            try {
                setStatus(intent.getStringExtra("state"));
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {

                String duration = intent.getStringExtra("duration");
                String lastPacketReceive = intent.getStringExtra("lastPacketReceive");
                String byteIn = intent.getStringExtra("byteIn");
                String byteOut = intent.getStringExtra("byteOut");

                if (duration == null) duration = "00:00:00";
                if (lastPacketReceive == null) lastPacketReceive = "0";
                if (byteIn == null) byteIn = " ";
                if (byteOut == null) byteOut = " ";
                updateConnectionStatus(duration, lastPacketReceive, byteIn, byteOut);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    };

    public void setStatus(String connectionState) {
        if (connectionState!= null) {
            switch (connectionState) {
                case "DISCONNECTED":
                    vpnStart = false;
                    vpnService.setDefaultStatus();
                    break;
                case "CONNECTED":
                    vpnStart = true;// it will use after restart this activity

                    break;
            }
            if (listener != null) listener.onVPNStatusChanged(connectionState);
        }else{

        }

    }

    public void updateConnectionStatus(String duration, String lastPacketReceive, String byteIn, String byteOut) {
        //binding.durationTv.setText("Duration: " + duration);
        //binding.lastPacketReceiveTv.setText("Packet Received: " + lastPacketReceive + " second ago");
        //binding.byteInTv.setText("Bytes In: " + byteIn);
        //binding.byteOutTv.setText("Bytes Out: " + byteOut);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if(listener != null) listener.onProfileLoaded(true);
        } else {
            if(listener != null) listener.onProfileLoaded(false);
        }

        finish();
    }
}
