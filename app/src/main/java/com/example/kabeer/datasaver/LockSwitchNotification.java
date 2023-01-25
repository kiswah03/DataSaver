package com.example.kabeer.datasaver;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

public class LockSwitchNotification {
    private static final String NOTIFICATION_TAG = "LockSwitch";

    public static void notify(final Context context,String msg,int imageId) {

        RemoteViews contentView = new RemoteViews(context.getPackageName(),R.layout.notificationlayout);
        contentView.setImageViewResource(R.id.imageappicon, R.mipmap.myapp_round);
        contentView.setTextViewText(R.id.maintxt, "Internet Lock");
        contentView.setTextViewText(R.id.locktxt, msg);
        contentView.setImageViewResource(R.id.imagebtn,imageId);

        Intent buttonIntent = new Intent(context, lockButtonListener.class);
        PendingIntent pendingButtonIntent = PendingIntent.getBroadcast(context, 3, buttonIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        contentView.setOnClickPendingIntent(R.id.imagebtn, pendingButtonIntent);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_stat_lock_switch)
                .setContent(contentView)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true);

        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_NO_CLEAR;
        notify(context, notification);
    }

    @TargetApi(Build.VERSION_CODES.ECLAIR)
    private static void notify(final Context context, final Notification notification) {
        final NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
            nm.notify(NOTIFICATION_TAG, 0, notification);

        } else {
            nm.notify(NOTIFICATION_TAG.hashCode(), notification);
        }
    }

    @TargetApi(Build.VERSION_CODES.ECLAIR)
    public static void cancel(final Context context) {
        final NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
            Intent buttonIntent = new Intent(context, lockButtonListener.class);
            PendingIntent.getBroadcast(context, 3, buttonIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT).cancel();
            nm.cancel(NOTIFICATION_TAG, 0);
        } else {
            Intent buttonIntent = new Intent(context, lockButtonListener.class);
            PendingIntent.getBroadcast(context, 3, buttonIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT).cancel();
            nm.cancel(NOTIFICATION_TAG.hashCode());
        }
    }
}
