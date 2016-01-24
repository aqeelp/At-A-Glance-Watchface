package io.github.aqeelp.ataglance;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by aqeelp on 1/23/16.
 */
public class NotificationListener extends NotificationListenerService {
    private String TAG = "NotificationListener";

    ArrayList<String> textIds = new ArrayList<>();
    ArrayList<String> messageIds = new ArrayList<>();
    int snaps;
    int gmails;

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {

        /*Log.i(TAG, "Notification posted:");
        Log.i(TAG, "ID: " + sbn.getId());
        Log.i(TAG, "Package name: " + sbn.getPackageName());
        Log.i(TAG, "Notification text: " + sbn.getNotification().tickerText);
        Log.i(TAG, " ");*/

        if (this.textIds == null) init();

        String name = sbn.getPackageName();

        if (name.equals("com.textra")) this.addTextra(sbn);
        if (name.equals("com.facebook.orca")) this.addMessenger(sbn);
        if (name.equals("com.snapchat.android")) this.addSnapchat(sbn);
        if (name.equals("com.google.android.gm")) this.addGmail(sbn);

        Intent i = new Intent("io.github.aqeelp.UPDATE_NOTIFICATIONS");
        i.putExtra("notifs", currentNotifications());
        sendBroadcast(i);
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

        Intent i = new Intent("io.github.aqeelp.UPDATE_NOTIFICATIONS");
        i.putExtra("notifs", currentNotifications());
        sendBroadcast(i);
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
    }

    // TODO: right procedure, but this info should be broadcasted on every update
    private Bundle currentNotifications() {
        Bundle notifs = new Bundle();

        notifs.putInt("Textra", this.textIds.size());
        notifs.putInt("Messenger", this.messageIds.size());
        notifs.putInt("Snapchat", this.snaps);
        notifs.putInt("Emails", this.gmails);

        return notifs;
    }
}
