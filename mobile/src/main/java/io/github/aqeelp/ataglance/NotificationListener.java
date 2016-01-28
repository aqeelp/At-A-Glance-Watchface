package io.github.aqeelp.ataglance;

import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;

/**
 * Created by aqeelp on 1/23/16.
 */
public class NotificationListener extends NotificationListenerService implements
        GoogleApiClient.ConnectionCallbacks {
    private final String TAG = "myTag";
    private static final String PATH = "/glance/notifs";

    ArrayList<String> textIds = new ArrayList<>();
    ArrayList<String> messageIds = new ArrayList<>();
    int snaps;
    int gmails;
    private GoogleApiClient mGoogleApiClient;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Notification Listener service started - making STICKY.");

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Notification Listener service created.");

        /*Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);*/

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .build();
        mGoogleApiClient.connect();

        this.init();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mGoogleApiClient != null)
            mGoogleApiClient.disconnect();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {

        Log.i(TAG, "Notification posted:");
        Log.i(TAG, "ID: " + sbn.getId());
        Log.i(TAG, "Package name: " + sbn.getPackageName());
        Log.i(TAG, "Notification text: " + sbn.getNotification().tickerText);
        Log.i(TAG, " ");

        if (this.textIds == null) init();

        String name = sbn.getPackageName();

        if (name.equals("com.textra")) this.addTextra(sbn);
        if (name.equals("com.facebook.orca")) this.addMessenger(sbn);
        if (name.equals("com.snapchat.android")) this.addSnapchat(sbn);
        if (name.equals("com.google.android.gm")) this.addGmail(sbn);

        /*Intent i = new Intent("io.github.aqeelp.UPDATE_NOTIFICATIONS");
        i.putExtra("notifs", currentNotifications());
        sendBroadcast(i);*/

        sendMessage(currentNotifications());
    }

    private void addTextra(StatusBarNotification sbn) {
        if (sbn.getId() > 1000000) return;

        if (this.textIds.contains(sbn.getId() + "")) return;

        this.textIds.add(sbn.getId() + "");
    }

    private void addMessenger(StatusBarNotification sbn) {
        // TODO: handle group chat

        String sender = (String) sbn.getNotification().tickerText;
        sender = sender.split(":")[0];

        if (this.messageIds.contains(sender)) return;

        this.messageIds.add(sender);
    }

    private void addSnapchat(StatusBarNotification sbn) {
        this.snaps++;
    }

    // TODO: handle inbox as well
    private void addGmail(StatusBarNotification sbn) {
        if (this.gmails == 0) this.gmails++;
        else {
            String content = (String) sbn.getNotification().tickerText;
            this.gmails = Integer.parseInt(content.split(" ")[0]);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        /*Log.i(TAG, "Notification removed:");
        Log.i(TAG, "ID: " + sbn.getId());
        Log.i(TAG, "Package name: " + sbn.getPackageName());
        Log.i(TAG, "Notification text: " + sbn.getNotification().tickerText);
        Log.i(TAG, " ");*/

        String name = sbn.getPackageName();

        if (this.textIds == null) this.init();

        if (name.equals("com.textra")) this.removeTextra(sbn);
        if (name.equals("com.facebook.orca")) this.removeMessenger(sbn);
        if (name.equals("com.snapchat.android")) this.removeSnapchat(sbn);
        if (name.equals("com.google.android.gm")) this.removeGmail(sbn);

        /*Intent i = new Intent("io.github.aqeelp.UPDATE_NOTIFICATIONS");
        i.putExtra("notifs", currentNotifications());
        sendBroadcast(i);*/

        sendMessage(currentNotifications());
    }

    private void removeTextra(StatusBarNotification sbn) {
        if (sbn.getId() > 1000000) return;

        if (this.textIds.contains(sbn.getId() + "")) {
            this.textIds.remove(sbn.getId() + "");
        }
    }

    private void removeMessenger(StatusBarNotification sbn) {
        // TODO: handle group chat

        String sender = (String) sbn.getNotification().tickerText;
        sender = sender.split(":")[0];

        if (this.messageIds.contains(sender)) {
            this.messageIds.remove(sender);
        }
    }

    private void removeSnapchat(StatusBarNotification sbn) {
        this.snaps = 0;
    }

    private void removeGmail(StatusBarNotification sbn) {
        this.gmails = 0;
    }

    private void init() {
        this.snaps = 0;
        this.gmails = 0;
        this.textIds = new ArrayList<>();
        this.messageIds = new ArrayList<>();

        // TODO: Broadcast initial values, or
    }

    private DataMap currentNotifications() {
        DataMap notifs = new DataMap();
        //Bundle notifs = new Bundle();

        notifs.putInt("textra", this.textIds.size());
        notifs.putInt("messenger", this.messageIds.size());
        notifs.putInt("snapchat", this.snaps);
        notifs.putInt("emails", this.gmails);

        return notifs;
    }

    private void sendMessage(DataMap dataMap) {
        Log.d(TAG, "Attempting to send notification update... " + dataMap);
        final byte[] rawData = dataMap.toByteArray();

        new Thread( new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes( mGoogleApiClient ).await();
                for(Node node : nodes.getNodes()) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            mGoogleApiClient, node.getId(), PATH, rawData).await();
                    if (result.getStatus().isSuccess())
                        Log.d(TAG, "Message sent successfully");
                    else
                        Log.d(TAG, "Message failed");
                }
            }
        }).start();
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
}
