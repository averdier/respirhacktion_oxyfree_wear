package dev.rastadev.oxyfree;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class WearService extends WearableListenerService {

    private SharedPreferences sharedpreferences;
    public static final String stepPref = "step";
    private final static String TAG = WearService.class.getCanonicalName();
    protected GoogleApiClient apiClient;
    private int current = 0;

    //Bluetooth
    private BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private Set<BluetoothDevice> pairedDevices;
    public static String EXTRA_ADDRESS = "device_address";
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    public void onCreate() {
        super.onCreate();
        sharedpreferences = getSharedPreferences(stepPref, Context.MODE_PRIVATE);
        if (sharedpreferences.contains("current")) {
            current = sharedpreferences.getInt("current", 0);
        }
        apiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        apiClient.connect();

        sharedpreferences.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                Log.d("SERVICE_APP", "Shared pref changed");
                if (key.equals("current")) {
                    current = sharedPreferences.getInt("current", 0);
                    sendMessage("step", String.valueOf(current));
                    Log.d("SERVICE_APP", "Send " + String.valueOf(current) + " to wear");
                }
            }
        });

        myBluetooth = BluetoothAdapter.getDefaultAdapter();

        if (myBluetooth != null && myBluetooth.isEnabled()) {
            Log.d("SERVICE_APP", "OK for bluetooth");
            BluetoothDevice device = myBluetooth.getRemoteDevice("00:18:E4:34:BE:44");
            if (device != null) {
                try {
                    btSocket = device.createRfcommSocketToServiceRecord(myUUID);
                    //btSocket = device.createInsecureRfcommSocketToServiceRecord(myUUID);
                    Log.d("SERVICE_APP", "Socket created");
                } catch (IOException e) {
                    Log.e("SERVICE_APP", "Unable to create socket");
                    e.printStackTrace();
                }
            }
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
            if (btSocket != null) {
                try {
                    btSocket.connect();
                    Log.d("SERVICE_APP", "Bluetooth connected");
                } catch (IOException e) {
                    Log.e("SERVICE_APP", "Unable to connect");
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        apiClient.disconnect();
        if (btSocket != null) {
            try {
                btSocket.close();
            } catch (IOException e) {
                Log.e("SERVICE_APP", "Unable to close socket");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        ConnectionResult result = apiClient.blockingConnect(30, TimeUnit.SECONDS);

        if (!result.isSuccess()) {
            Log.e(TAG, "Failed to connect to GoogleApiClient.");
            return;
        }

        final String path = messageEvent.getPath();
        if (path.equals("step")) {
           Log.d("SERVICE_APP", "Receive from wear " + new String(messageEvent.getData()));
           int old = current;
           current = Integer.valueOf(new String(messageEvent.getData()));
           updatePrefs();
           if (btSocket != null) {
               try {
                   if (old < current) btSocket.getOutputStream().write("i".getBytes());
                   else btSocket.getOutputStream().write("d".getBytes());
                   Log.d("SERVICE_APP", "Bluetooth payload send");
               } catch (IOException e) {
                   Log.e("SERVICE_APP", "Unable to send command");
                   e.printStackTrace();
               }
           }
        }
    }

    protected void sendMessage(final String path, final String message) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(apiClient).await();
                for (Node node : nodes.getNodes()) {
                    Wearable.MessageApi.sendMessage(apiClient, node.getId(), path, message.getBytes()).await();
                }
            }
        }).start();
    }

    private void updatePrefs () {
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putInt("current", current);
        editor.apply();
    }
}
