package cloud.sudheer.com.internetofthings;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.parse.ParsePushBroadcastReceiver;

/**
 * Created by I14746 on 1/26/2015.
 */
public class Receiver extends ParsePushBroadcastReceiver {

    @Override
    public void onPushReceive(Context context, Intent intent) {
            Log.e("Push", "Clicked");
            Intent i = new Intent(context.getApplicationContext(), MainScreenActivity.class);
            i.putExtras(intent.getExtras());
//            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP |
                Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
//          RefreshStatus();
    }
}