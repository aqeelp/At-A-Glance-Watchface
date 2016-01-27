package io.github.aqeelp.ataglance;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

public class MainActivity extends AppCompatActivity implements
        ResultCallback<DataApi.DataItemResult>,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "myTag";
    private static final String PATH = "/watch_face/notifs";

    private NotificationReceiver receiver;
    private String peerId;
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
        startActivity(intent);

        receiver = new NotificationReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("io.github.aqeelp.UPDATE_NOTIFICATIONS");
        registerReceiver(receiver, filter);

        peerId = getIntent().getStringExtra("android.support.wearable.watchface.extra.PEER_ID");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

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
        mGoogleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
    public void onConnected(Bundle bundle) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onConnected: " + bundle);
        }

        if (peerId != null) {
            Uri.Builder builder = new Uri.Builder();
            Uri uri = builder.scheme("wear").path(PATH).authority(peerId).build();
            Wearable.DataApi.getDataItem(mGoogleApiClient, uri).setResultCallback(this);
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

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onConnectionFailed: " + connectionResult);
        }
    }

    @Override
    public void onResult(DataApi.DataItemResult dataItemResult) {
        if (dataItemResult.getStatus().isSuccess() && dataItemResult.getDataItem() != null) {
            DataItem configDataItem = dataItemResult.getDataItem();
            DataMapItem dataMapItem = DataMapItem.fromDataItem(configDataItem);
            DataMap config = dataMapItem.getDataMap();
            Log.d(TAG, "Data received:onResult - " + config);
        } else {
            // If DataItem with the current config can't be retrieved, select the default items on
            // each picker.
            // setUpAllPickers(null);
            Log.d(TAG, "Data received:onResult - " + dataItemResult);
        }
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
        if (peerId != null) {
            byte[] rawData = dataMap.toByteArray();
            Wearable.MessageApi.sendMessage(mGoogleApiClient, peerId, PATH, rawData);

            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Sent watch face: " + dataMap);
            }
        }
    }
}
