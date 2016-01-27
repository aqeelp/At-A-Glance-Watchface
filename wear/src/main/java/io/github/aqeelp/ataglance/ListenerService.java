package io.github.aqeelp.ataglance;

import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by aqeelp on 1/26/16.
 */
public class ListenerService extends WearableListenerService {
    public ListenerService() {
        super();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.v("myTag", "Change Received!!!!!");

        DataMap dataMap;
        for (DataEvent event : dataEvents) {

            // Check the data type
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // Check the data path
                String path = event.getDataItem().getUri().getPath();
                if (path.equals("/notifs")) {
                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    GlanceFace.parseNotifPackage(dataMap);
                } else if (path.equals("/test")) {
                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    if (dataMap.getInt("testing") == 1)
                        Log.v("myTag", "received! success");
                    else
                        Log.v("myTag", "received, not correct value though");
                }
            }
        }
    }
}
