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
            "keluarga_zana_calls_v3";

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

            String type = data.get("type");

            /*
             * Untuk panggilan kita tidak hanya
             * mengandalkan WakeLock.
             *
             * Full-Screen Intent akan dipakai.
             */

            if ("incoming_call".equals(type)) {

                /*
                 * V9:
                 * Panggilan juga harus membangunkan layar
                 * sebelum Full Screen Notification dijalankan.
                 */
                wakeScreen();

                android.util.Log.i(
                        TAG,
                        "WAKE SCREEN UNTUK INCOMING CALL"
                );

                showIncomingCall(data);

            } else {

                wakeScreen();
                showNormalNotification(data);
            }

        } catch (Throwable e) {

            android.util.Log.e(
                    TAG,
                    "Gagal memproses FCM",
                    e
            );
        }
    }

    private void wakeScreen() {

        try {

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

            if (!screenOn) {

                PowerManager.WakeLock wakeLock =
                        powerManager.newWakeLock(
                                PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                                        | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                                TAG + ":WakeLock"
                        );

                try {

                    wakeLock.acquire(5000);

                } finally {

                    if (wakeLock.isHeld()) {
                        wakeLock.release();
                    }
                }
            }

        } catch (Throwable e) {

            android.util.Log.e(
                    TAG,
                    "WakeLock gagal",
                    e
            );
        }
    }

    private void showIncomingCall(
            Map<String, String> data) {

        NotificationManager manager =
                (NotificationManager)
                        getSystemService(
                                Context.NOTIFICATION_SERVICE
                        );

        if (manager == null) {
            return;
        }

        createCallChannel(manager);

        Intent intent =
                new Intent(
                        this,
                        MainActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                );

        intent.putExtra(
                "incoming_call",
                true
        );

        intent.putExtra(
                "callId",
                data.get("callId")
        );

        intent.putExtra(
                "callType",
                data.get("callType")
        );

        intent.putExtra(
                "fromUserId",
                data.get("fromUserId")
        );

        intent.putExtra(
                "fromName",
                data.get("fromName")
        );

        PendingIntent fullScreenIntent =
                PendingIntent.getActivity(
                        this,
                        9001,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE
                );

        String from =
                data.get("fromName");

        if (from == null ||
                from.trim().isEmpty()) {

            from = "Keluarga Zana";
        }

        String body;

        if ("video".equals(
                data.get("callType"))) {

            body =
                    from +
                    " mengajak video call";

        } else {

            body =
                    from +
                    " mengajak telepon";
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(
                        this,
                        CALL_CHANNEL
                )
                .setSmallIcon(
                        android.R.drawable.ic_menu_call
                )
                .setContentTitle(
                        "Panggilan masuk"
                )
                .setContentText(body)
                .setStyle(
                        new NotificationCompat.BigTextStyle()
                                .bigText(body)
                )
                .setPriority(
                        NotificationCompat.PRIORITY_MAX
                )
                .setCategory(
                        NotificationCompat.CATEGORY_CALL
                )
                .setVisibility(
                        NotificationCompat.VISIBILITY_PUBLIC
                )
                .setOngoing(true)
                .setAutoCancel(false)
                .setFullScreenIntent(
                        fullScreenIntent,
                        true
                )
                .setContentIntent(
                        fullScreenIntent
                )
                .setDefaults(
                        NotificationCompat.DEFAULT_ALL
                );

        manager.notify(
                9001,
                builder.build()
        );

        android.util.Log.i(
                TAG,
                "FULL SCREEN CALL NOTIFICATION DIBUAT"
        );
    }

    private void showNormalNotification(
            Map<String, String> data) {

        NotificationManager manager =
                (NotificationManager)
                        getSystemService(
                                Context.NOTIFICATION_SERVICE
                        );

        if (manager == null) {
            return;
        }

        createMessageChannel(manager);

        String title =
                data.get("title");

        String body =
                data.get("body");

        if (title == null ||
                title.trim().isEmpty()) {

            title = "Keluarga Zana";
        }

        if (body == null ||
                body.trim().isEmpty()) {

            body = "Ada pesan baru.";
        }

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
                        MESSAGE_CHANNEL
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
                        NotificationCompat.PRIORITY_HIGH
                )
                .setCategory(
                        NotificationCompat.CATEGORY_MESSAGE
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
    }

    private void createMessageChannel(
            NotificationManager manager) {

        if (Build.VERSION.SDK_INT <
                Build.VERSION_CODES.O) {

            return;
        }

        if (manager.getNotificationChannel(
                MESSAGE_CHANNEL) != null) {

            return;
        }

        NotificationChannel channel =
                new NotificationChannel(
                        MESSAGE_CHANNEL,
                        "Pesan Keluarga Zana",
                        NotificationManager.IMPORTANCE_HIGH
                );

        channel.enableVibration(true);

        channel.setVibrationPattern(
                new long[]{
                        0,300,200,300
                }
        );

        Uri soundUri =
                RingtoneManager.getDefaultUri(
                        RingtoneManager.TYPE_NOTIFICATION
                );

        AudioAttributes attributes =
                new AudioAttributes.Builder()
                        .setUsage(
                                AudioAttributes.USAGE_NOTIFICATION
                        )
                        .setContentType(
                                AudioAttributes.CONTENT_TYPE_SONIFICATION
                        )
                        .build();

        channel.setSound(
                soundUri,
                attributes
        );

        channel.setLockscreenVisibility(
                android.app.Notification.VISIBILITY_PUBLIC
        );

        manager.createNotificationChannel(channel);
    }

    private void createCallChannel(
            NotificationManager manager) {

        if (Build.VERSION.SDK_INT <
                Build.VERSION_CODES.O) {

            return;
        }

        if (manager.getNotificationChannel(
                CALL_CHANNEL) != null) {

            return;
        }

        NotificationChannel channel =
                new NotificationChannel(
                        CALL_CHANNEL,
                        "Panggilan Keluarga Zana",
                        NotificationManager.IMPORTANCE_HIGH
                );

        channel.setDescription(
                "Panggilan masuk Keluarga Zana"
        );

        channel.enableVibration(true);

        channel.setVibrationPattern(
                new long[]{
                        0,
                        700,
                        500,
                        700,
                        500,
                        700
                }
        );

        Uri soundUri =
                RingtoneManager.getDefaultUri(
                        RingtoneManager.TYPE_RINGTONE
                );

        AudioAttributes attributes =
                new AudioAttributes.Builder()
                        .setUsage(
                                AudioAttributes.USAGE_NOTIFICATION_RINGTONE
                        )
                        .setContentType(
                                AudioAttributes.CONTENT_TYPE_SONIFICATION
                        )
                        .build();

        channel.setSound(
                soundUri,
                attributes
        );

        channel.setLockscreenVisibility(
                android.app.Notification.VISIBILITY_PUBLIC
        );

        manager.createNotificationChannel(channel);
    }
}
