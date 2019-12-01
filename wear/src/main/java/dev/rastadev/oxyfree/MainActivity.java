package dev.rastadev.oxyfree;

import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

public class MainActivity extends WearableActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, MessageApi.MessageListener {

    private final int maxSteps = 6;
    private int current = 0;
    private TextView mTextView;
    protected GoogleApiClient apiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = findViewById(R.id.main_text_current);
        // Enables Always-on
        setAmbientEnabled();
        updateUI();
    }

    @Override
    protected void onStart() {
        super.onStart();
        apiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        apiClient.connect();
    }

    public void onPreviousClick(View view) {
        Log.d("WEAR_APP", "Previous clicked");
        if (current > 0) {
            current -= 1;
            updateUI();
            sendMessage("step", String.valueOf(current));
        }
    }

    public void onNextClick(View view) {
        Log.d("WEAR_APP", "Next clicked");
        if (current < maxSteps) {
            current += 1;
            updateUI();
            sendMessage("step", String.valueOf(current));
        }
    }

    private void updateUI () {
        mTextView.setText(String.valueOf(current));
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Wearable.MessageApi.addListener(apiClient, this);
        Log.d("WEAR_APP", "Connected to phone");
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        final String path = messageEvent.getPath();
        Log.d("WEAR_APP", path);

        if (path.equals("step")) {
            Log.d("WEAR_APP", "Receive " + new String(messageEvent.getData()));
            current = Integer.valueOf(new String(messageEvent.getData()));
            updateUI();
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
}
