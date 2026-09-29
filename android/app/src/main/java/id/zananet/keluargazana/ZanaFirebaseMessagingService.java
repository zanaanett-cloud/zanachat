package id.zananet.keluargazana;

import android.app.KeyguardManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class ZanaFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "ZANA_FCM_WAKE";

    private static final String CHANNEL_ID =
            "keluarga_zana_messages";

    private static final String CHANNEL_NAME =
            "Pesan Keluarga Zana";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        try {

            wakeScreen();

        } catch (Throwable e) {

            android.util.Log.e(
                    TAG,
                    "Gagal membangunkan layar",
                    e
            );
        }

        try {

            showNotification(remoteMessage);

        } catch (Throwable e) {

            android.util.Log.e(
                    TAG,
                    "Gagal membuat notifikasi",
                    e
            );
        }
    }

    private void wakeScreen() {

        PowerManager powerManager =
                (PowerManager) getSystemService(
                        Context.POWER_SERVICE
                );

        if (powerManager == null) {
            return;
        }

        boolean screenOn;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            screenOn = powerManager.isInteractive();
        } else {
            screenOn = powerManager.isScreenOn();
        }

        if (screenOn) {
            return;
        }

        PowerManager.WakeLock wakeLock =
                powerManager.newWakeLock(
                        PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                                | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                        TAG + ":WakeLock"
                );

        wakeLock.acquire(5000);

        if (wakeLock.isHeld()) {
            wakeLock.release();
        }

        android.util.Log.i(
                TAG,
                "LAYAR DIBANGUNKAN"
        );
    }

    private void showNotification(
            RemoteMessage remoteMessage
    ) {

        NotificationManager notificationManager =
                (NotificationManager)
                        getSystemService(
                                Context.NOTIFICATION_SERVICE
                        );

        if (notificationManager == null) {
            return;
        }

        createChannel(notificationManager);

        String title =
                remoteMessage.getNotification() != null
                        && remoteMessage.getNotification().getTitle() != null
                        ? remoteMessage.getNotification().getTitle()
                        : "Keluarga Zana";

        String body =
                remoteMessage.getNotification() != null
                        && remoteMessage.getNotification().getBody() != null
                        ? remoteMessage.getNotification().getBody()
                        : "Ada pesan baru.";

        Intent intent =
                new Intent(
                        this,
                        MainActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_SINGLE_TOP
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
        );

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        this,
                        1001,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE
                );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(
                        this,
                        CHANNEL_ID
                )
                .setSmallIcon(
                        getApplicationInfo().icon
                )
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(
                        NotificationCompat.PRIORITY_HIGH
                )
                .setCategory(
                        NotificationCompat.CATEGORY_MESSAGE
                )
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVisibility(
                        NotificationCompat.VISIBILITY_PUBLIC
                );

        notificationManager.notify(
                (int) System.currentTimeMillis(),
                builder.build()
        );
    }

    private void createChannel(
            NotificationManager notificationManager
    ) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationChannel channel =
                new NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                );

        channel.setDescription(
                "Notifikasi pesan Keluarga Zana"
        );

        channel.enableVibration(true);

        channel.setLockscreenVisibility(
                android.app.Notification.VISIBILITY_PUBLIC
        );

        notificationManager.createNotificationChannel(
                channel
        );
    }
}
