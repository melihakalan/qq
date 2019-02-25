package com.quiqueapp.qq;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * Created by user on 24.2.2016.
 */
public class AlarmReceiver extends BroadcastReceiver {
    public static final int REQUEST_CODE = 8095;

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle b = intent.getExtras();
        Intent it = new Intent(context, NotificationService.class);
        it.putExtras(b);
        context.startService(it);
    }
}
