package io.github.aqeelp.ataglance;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by aqeelp on 1/29/16.
 */
public class ApplicationLauncher extends WearableListenerService {
    private static final String TAG = "myTag";
    private static final String APP_LAUNCH_PATH = "/glance/app_launch";

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "Mobile-side messaging receiver created.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Application Launcher service started - making STICKY.");

        return START_STICKY;
    }

    @Override // WearableListenerService
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "Message received!");

        if (messageEvent.getPath().equalsIgnoreCase(APP_LAUNCH_PATH)) {
            byte[] rawData = messageEvent.getData();
            DataMap dataMap = DataMap.fromByteArray(rawData);
            String packageName = dataMap.getString("package");
            Intent intentToStart = getPackageManager().getLaunchIntentForPackage(packageName);
            intentToStart.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intentToStart);
        }
    }
}
