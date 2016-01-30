package io.github.aqeelp.ataglance;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "myTag";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
        startActivity(intent);

        Log.d(TAG, "Starting up Notification Listener service...");
        Intent notificationServiceStarter = new Intent(this, NotificationListener.class);
        startService(notificationServiceStarter);

        Log.d(TAG, "Starting up mobile-side message receiver...");
        Intent applicationLauncherStarter = new Intent(this, ApplicationLauncher.class);
        startService(applicationLauncherStarter);

        finish();
    }
}
