package com.vecolsoft.deligo.Utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.vecolsoft.deligo.R;

public class NotificationHelper extends ContextWrapper {

    private static final String DELIGO_CHANEL_ID = "com.vecolsoft.deligo";
    private static final String VENECOlSOFT_NAME = "VECOLSOFT Deligo";

    private NotificationManager manager;

    public NotificationHelper(Context base) {
        super(base);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createchannels();

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createchannels() {

        NotificationChannel venecolsoft = new NotificationChannel(DELIGO_CHANEL_ID,
                VENECOlSOFT_NAME,
                NotificationManager.IMPORTANCE_DEFAULT);
        venecolsoft.enableLights(true);
        venecolsoft.enableVibration(true);
        venecolsoft.setLightColor(Color.GRAY);
        venecolsoft.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        getManager().createNotificationChannel(venecolsoft);

    }

    public NotificationManager getManager() {

        if (manager == null)
            manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        return manager;

    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    public Notification.Builder getDeLIGONotification(String title, String content, PendingIntent contentIntent,
                                                       Uri soundUri)
    {
        return new Notification.Builder(getApplicationContext(), DELIGO_CHANEL_ID)
                .setContentText(content)
                .setContentTitle(title)
                .setAutoCancel(true)
                .setSound(soundUri)
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ic_moto);
    }

}
