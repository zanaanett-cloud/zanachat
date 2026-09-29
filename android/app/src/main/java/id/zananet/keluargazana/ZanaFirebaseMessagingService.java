package id.zananet.keluargazana;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class ZanaFirebaseMessagingService
        extends FirebaseMessagingService {

    private static final String TAG =
            "ZANA_FCM_WAKE";

    private static final String MESSAGE_CHANNEL =
            "keluarga_zana_messages_v2";

    private static final String CALL_CHANNEL =
            "keluarga_zana_calls_v2";

    @Override
    public void onMessageReceived(
            RemoteMessage remoteMessage) {

        android.util.Log.i(
                TAG,
                "FCM DATA DITERIMA"
        );

        try {
            Map<String, String> data =
                    remoteMessage.getData();

            android.util.Log.i(
                    TAG,
                    "FCM DATA = " + data
            );

            wakeScreen();

            showNotification(data);

        } catch (Throwable e) {
            android.util.Log.e(
                    TAG,
                    "Gagal memproses FCM",
                    e
            );
        }
    }

    private void wakeScreen() {

        PowerManager powerManager =
                (PowerManager)
                        getSystemService(
                                Context.POWER_SERVICE
                        );

        if (powerManager == null) {
            return;
        }

        boolean screenOn;

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.KITKAT_WATCH) {

            screenOn =
                    powerManager.isInteractive();

        } else {

            screenOn =
                    powerManager.isScreenOn();
        }

        android.util.Log.i(
                TAG,
                "Screen sebelum wake = " + screenOn
        );

        if (!screenOn) {

            PowerManager.WakeLock wakeLock =
                    powerManager.newWakeLock(
                            PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                                    | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                            TAG + ":WakeLock"
                    );

            try {

                wakeLock.acquire(5000);

                android.util.Log.i(
                        TAG,
                        "WAKE LOCK ACQUIRED"
                );

            } finally {

                if (wakeLock.isHeld()) {
                    wakeLock.release();
                }
            }
        }
    }

    private void showNotification(
            Map<String, String> data) {

        String type = data.get("type");
        String title = data.get("title");
        String body = data.get("body");

        if (title == null ||
                title.trim().isEmpty()) {

            title =
                    "incoming_call".equals(type)
                            ? "Panggilan masuk"
                            : "Keluarga Zana";
        }

        if (body == null ||
                body.trim().isEmpty()) {

            if ("incoming_call".equals(type)) {

                String from =
                        data.get("fromName");

                if (from == null ||
                        from.trim().isEmpty()) {

                    from = "Keluarga Zana";
                }

                body =
                        from +
                        ("video".equals(
                                data.get("callType"))
                                ? " mengajak video call"
                                : " mengajak telepon");

            } else {

                body = "Ada pesan baru.";
            }
        }

        String channelId =
                "incoming_call".equals(type)
                        ? CALL_CHANNEL
                        : MESSAGE_CHANNEL;

        NotificationManager manager =
                (NotificationManager)
                        getSystemService(
                                Context.NOTIFICATION_SERVICE
                        );

        if (manager == null) {
            return;
        }

        createChannel(
                manager,
                channelId,
                "incoming_call".equals(type)
        );

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
                        (int)
                                System.currentTimeMillis(),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE
                );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(
                        this,
                        channelId
                )
                .setSmallIcon(
                        android.R.drawable.ic_dialog_info
                )
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(
                        new NotificationCompat.BigTextStyle()
                                .bigText(body)
                )
                .setPriority(
                        NotificationCompat.PRIORITY_MAX
                )
                .setCategory(
                        "incoming_call".equals(type)
                                ? NotificationCompat.CATEGORY_CALL
                                : NotificationCompat.CATEGORY_MESSAGE
                )
                .setVisibility(
                        NotificationCompat.VISIBILITY_PUBLIC
                )
                .setAutoCancel(true)
                .setContentIntent(
                        pendingIntent
                )
                .setDefaults(
                        NotificationCompat.DEFAULT_ALL
                );

        manager.notify(
                (int)
                        (System.currentTimeMillis()
                                & 0x7fffffff),
                builder.build()
        );

        android.util.Log.i(
                TAG,
                "NOTIFIKASI DIBUAT: " +
                        title +
                        " / " +
                        body
        );
    }

    private void createChannel(
            NotificationManager manager,
            String channelId,
            boolean isCall) {

        if (Build.VERSION.SDK_INT <
                Build.VERSION_CODES.O) {

            return;
        }

        NotificationChannel channel =
                manager.getNotificationChannel(
                        channelId
                );

        if (channel != null) {
            return;
        }

        String name =
                isCall
                        ? "Panggilan Keluarga Zana"
                        : "Pesan Keluarga Zana";

        channel =
                new NotificationChannel(
                        channelId,
                        name,
                        NotificationManager.IMPORTANCE_HIGH
                );

        channel.enableVibration(true);

        channel.setVibrationPattern(
                isCall
                        ? new long[]{
                            0,700,500,700,500,700
                        }
                        : new long[]{
                            0,300,200,300
                        }
        );

        Uri soundUri =
                RingtoneManager.getDefaultUri(
                        RingtoneManager.TYPE_NOTIFICATION
                );

        AudioAttributes audioAttributes =
                new AudioAttributes.Builder()
                        .setUsage(
                                isCall
                                        ? AudioAttributes.USAGE_NOTIFICATION_RINGTONE
                                        : AudioAttributes.USAGE_NOTIFICATION
                        )
                        .setContentType(
                                AudioAttributes.CONTENT_TYPE_SONIFICATION
                        )
                        .build();

        channel.setSound(
                soundUri,
                audioAttributes
        );

        channel.setLockscreenVisibility(
                android.app.Notification.VISIBILITY_PUBLIC
        );

        manager.createNotificationChannel(channel);
    }
}
