package com.cutecomm.liumm.screenrecorddemo;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;

/**
 * Created by 25817 on 2016/11/17.
 */

public class RecordService extends Service {

    private static final int NOTIFICATION_ID = 21313;

    public static final String ACTION_START = "com.liumm.START";
    public static final String ACTION_STOP = "com.liumm.STOP";

    @Override
    public void onCreate() {
        super.onCreate();
        stopForeground(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();

            if (!TextUtils.isEmpty(action)) {
                if (ACTION_START.equals(action)) {
                    startRecord();
                } else if (ACTION_STOP.equals(action)) {
                    stopRecord();
                }
            }
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startRecord() {
        Notification.Builder builder = new Notification.Builder(this);
        builder.setOngoing(true);
        builder.setSmallIcon(android.R.drawable.stat_notify_more);
        builder.setContentTitle(getString(R.string.screen_recording));

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        builder.setContentIntent(pendingIntent);

        startForeground(NOTIFICATION_ID, builder.build());
    }

    private void stopRecord() {
        stopSelf();
    }
}
