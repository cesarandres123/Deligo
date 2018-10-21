package com.vecolsoft.deligo.Servicio;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.vecolsoft.deligo.Activitys.HomeBox;
import com.vecolsoft.deligo.R;

public class MyFirebaseMessaging extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(final RemoteMessage remoteMessage) {

        if (remoteMessage.getNotification().getTitle().equals("Cancel")) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    HomeBox.Cancelado();
                    Toast.makeText(MyFirebaseMessaging.this, "" +remoteMessage.getNotification().getBody() , Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (remoteMessage.getNotification().getTitle().equals("Aceptado")) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    HomeBox.Aceptado();
                    Toast.makeText(MyFirebaseMessaging.this, "" +remoteMessage.getNotification().getBody() , Toast.LENGTH_SHORT).show();
                }
            });
        }

        else if (remoteMessage.getNotification().getTitle().equals("Esta aqui!")) {

            ShowArrivedNotification(remoteMessage.getNotification().getBody());
        }




    }

    private void ShowArrivedNotification(String body) {
        PendingIntent contentIntent = PendingIntent.getActivity(getBaseContext(),
                0,new Intent(),PendingIntent.FLAG_ONE_SHOT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getBaseContext());

        builder.setAutoCancel(true)
                .setDefaults(android.app.Notification.DEFAULT_LIGHTS| android.app.Notification.DEFAULT_SOUND)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("Esta aqui!")
                .setContentText(body)
                .setContentIntent(contentIntent);
        NotificationManager manager = (NotificationManager)getBaseContext().getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(1,builder.build());

    }
}
