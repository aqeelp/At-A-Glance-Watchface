package io.github.aqeelp.ataglance;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks {

    private static final String TAG = "myTag";
    private static final String PATH = "/glance/notifs";

    private NotificationReceiver receiver;
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
        startActivity(intent);

        Log.d(TAG, "Starting up Notification Listener service...");
        Intent notificationServiceStarter = new Intent(this, NotificationListener.class);
        startService(notificationServiceStarter);

        finish();



        receiver = new NotificationReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("io.github.aqeelp.UPDATE_NOTIFICATIONS");
        registerReceiver(receiver, filter);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .build();
        mGoogleApiClient.connect();

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Sending a test...");

                DataMap dataMap = new DataMap();
                dataMap.putInt("textra", 1);
                dataMap.putInt("messenger", 2);
                dataMap.putInt("snapchat", 3);
                dataMap.putInt("email", 4);

                sendConfigUpdateMessage(dataMap);
            }
        });
    }

    // Connect to the data layer when the Activity starts
    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
            Log.d(TAG, "Connection started");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mGoogleApiClient != null)
            mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (null != mGoogleApiClient && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mGoogleApiClient != null)
            mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onConnected: " + bundle);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onConnectionSuspended: " + i);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Wearable.DataApi.removeListener(mGoogleApiClient, this);
        // mGoogleApiClient.disconnect();
    }

    private class NotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "Notification broadcast received...sending to watch");

            Bundle notifs = intent.getBundleExtra("notifs");

            DataMap dataMap = new DataMap();
            dataMap.putInt("textra", notifs.getInt("Textra"));
            dataMap.putInt("messenger", notifs.getInt("Messenger"));
            dataMap.putInt("snapchat", notifs.getInt("Snapchat"));
            dataMap.putInt("email", notifs.getInt("Emails"));

            sendConfigUpdateMessage(dataMap);
        }
    }

    private void sendConfigUpdateMessage(DataMap dataMap) {
        final byte[] rawData = dataMap.toByteArray();

        new Thread( new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes( mGoogleApiClient ).await();
                for(Node node : nodes.getNodes()) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            mGoogleApiClient, node.getId(), PATH, rawData).await();
                }
            }
        }).start();
    }
}
