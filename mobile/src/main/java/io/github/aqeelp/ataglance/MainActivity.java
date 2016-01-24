package io.github.aqeelp.ataglance;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

public class MainActivity extends AppCompatActivity {
    NotificationReceiver receiver;

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
    }

    class NotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v("Receiver", "Changed received...");

            Bundle notifs = intent.getBundleExtra("notifs");
            int textraCount = notifs.getInt("Textra");
            int messengerCount = notifs.getInt("Messenger");
            int snapchatCount = notifs.getInt("Snapchat");
            int emailCount = notifs.getInt("Emails");
        }
    }

}
