package com.example.hearthpaw.util;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.hearthpaw.R;

public class CareReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String petName = intent.getStringExtra(CareNotificationHelper.EXTRA_PET_NAME);
        String taskName = intent.getStringExtra(CareNotificationHelper.EXTRA_TASK_NAME);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CareNotificationHelper.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_paw)
                .setColor(ContextCompat.getColor(context, R.color.hearthpaw_primary))
                .setContentTitle("🐾 Pet Care: " + petName)
                .setContentText("Time for " + taskName + "! ❤️")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }
}
