package io.github.aqeelp.ataglance;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by aqeelp on 1/26/16.
 */
public class ListenerService extends WearableListenerService {
    private static final String TAG = "myTag";
    private static final String PATH = "/glance/notifs";

    @Override // WearableListenerService
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "Message received!");

        if (!messageEvent.getPath().equalsIgnoreCase(PATH)) {
            return;
        }

        byte[] rawData = messageEvent.getData();
        DataMap dataMap = DataMap.fromByteArray(rawData);

        GlanceFace.parseNotifPackage(dataMap);
    }
}
