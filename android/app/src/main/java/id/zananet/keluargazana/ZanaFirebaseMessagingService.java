package id.zananet.keluargazana;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class ZanaFirebaseMessagingService
        extends FirebaseMessagingService {

    private static final String TAG =
            "ZANA_FCM";

    private static final String CALL_CHANNEL =
            "keluarga_zana_calls_v4";

    private static final int CALL_NOTIFICATION_ID =
            9001;

    @Override
    public void onMessageReceived(
            @NonNull RemoteMessage remoteMessage
    ) {

        try {

            Map<String, String> data =
                    remoteMessage.getData();

            android.util.Log.i(
                    TAG,
                    "FCM DATA = " + data
            );

            String type =
                    data.get("type");

            if ("incoming_call".equals(type)) {

                android.util.Log.i(
                        TAG,
                        "INCOMING CALL V10"
                );

                /*
                 * Bangunkan layar SEBELUM
                 * membuat notification.
                 */
                wakeScreen();

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

            android.util.Log.i(
                    TAG,
                    "SCREEN INTERACTIVE=" + screenOn
            );

            if (!screenOn) {

                PowerManager.WakeLock wakeLock =
                        powerManager.newWakeLock(
                                PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                                        | PowerManager.ACQUIRE_CAUSES_WAKEUP
                                        | PowerManager.ON_AFTER_RELEASE,
                                TAG + ":IncomingCall"
                        );

                try {

                    wakeLock.acquire(10000);

                    android.util.Log.i(
                            TAG,
                            "WAKELOCK 10 DETIK BERHASIL"
                    );

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
            Map<String, String> data
    ) {

        NotificationManager manager =
                (NotificationManager)
                        getSystemService(
                                Context.NOTIFICATION_SERVICE
                        );

        if (manager == null) {
            return;
        }

        createCallChannel(manager);

        String callId =
                data.get("callId");

        if (callId == null ||
                callId.trim().isEmpty()) {

            callId =
                    "call-" +
                    System.currentTimeMillis();
        }

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
                callId
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

        PendingIntent fullScreenIntent;

        /*
         * Android 15:
         * PendingIntent yang dibuat oleh aplikasi harus
         * secara eksplisit mengizinkan background activity launch.
         *
         * Ini penting agar Full Screen Intent incoming call
         * benar-benar dapat membawa MainActivity ke depan
         * ketika HP sedang sleep/terkunci.
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {

            android.app.ActivityOptions options =
                    android.app.ActivityOptions.makeBasic();

            options.setPendingIntentCreatorBackgroundActivityStartMode(
                    android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
            );

            fullScreenIntent =
                    PendingIntent.getActivity(
                            this,
                            CALL_NOTIFICATION_ID,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                                    | PendingIntent.FLAG_IMMUTABLE,
                            options.toBundle()
                    );

            android.util.Log.i(
                    TAG,
                    "ANDROID 14/15 BAL CREATOR ENABLED"
            );

        } else {

            fullScreenIntent =
                    PendingIntent.getActivity(
                            this,
                            CALL_NOTIFICATION_ID,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                                    | PendingIntent.FLAG_IMMUTABLE
                    );
        }

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
                .setTimeoutAfter(60000)
                .setFullScreenIntent(
                        fullScreenIntent,
                        true
                )
                .setContentIntent(
                        fullScreenIntent
                )
                .setVibrate(
                        new long[]{
                                0,
                                500,
                                500,
                                500,
                                500,
                                500
                        }
                );

        /*
         * Hapus notifikasi panggilan lama
         * sebelum menampilkan yang baru.
         */
        manager.cancel(
                CALL_NOTIFICATION_ID
        );

        manager.notify(
                CALL_NOTIFICATION_ID,
                builder.build()
        );

        android.util.Log.i(
                TAG,
                "FULL SCREEN CALL NOTIFICATION V10 DITAMPILKAN callId="
                        + callId
        );
    }

    private void createCallChannel(
            NotificationManager manager
    ) {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CALL_CHANNEL,
                            "Panggilan Keluarga Zana",
                            NotificationManager.IMPORTANCE_HIGH
                    );

            channel.setDescription(
                    "Panggilan masuk Keluarga Zana"
            );

            channel.setLockscreenVisibility(
                    android.app.Notification.VISIBILITY_PUBLIC
            );

            channel.enableVibration(true);

            channel.setVibrationPattern(
                    new long[]{
                            0,
                            500,
                            500,
                            500,
                            500,
                            500
                    }
            );

            android.net.Uri ringtone =
                    android.media.RingtoneManager
                            .getDefaultUri(
                                    android.media.RingtoneManager
                                            .TYPE_RINGTONE
                            );

            android.media.AudioAttributes audioAttributes =
                    new android.media.AudioAttributes.Builder()
                            .setUsage(
                                    android.media.AudioAttributes
                                            .USAGE_NOTIFICATION_RINGTONE
                            )
                            .setContentType(
                                    android.media.AudioAttributes
                                            .CONTENT_TYPE_SONIFICATION
                            )
                            .build();

            channel.setSound(
                    ringtone,
                    audioAttributes
            );

            manager.createNotificationChannel(
                    channel
            );
        }
    }

    private void showNormalNotification(
            Map<String, String> data
    ) {

        NotificationManager manager =
                (NotificationManager)
                        getSystemService(
                                Context.NOTIFICATION_SERVICE
                        );

        if (manager == null) {
            return;
        }

        String channelId =
                "keluarga_zana_messages";

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            channelId,
                            "Pesan Keluarga Zana",
                            NotificationManager.IMPORTANCE_HIGH
                    );

            channel.setLockscreenVisibility(
                    android.app.Notification.VISIBILITY_PUBLIC
            );

            manager.createNotificationChannel(
                    channel
            );
        }

        Intent intent =
                new Intent(
                        this,
                        MainActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        this,
                        (int) System.currentTimeMillis(),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE
                );

        String title =
                data.get("title");

        String body =
                data.get("body");

        if (title == null) {
            title = "Keluarga Zana";
        }

        if (body == null) {
            body = "Pesan baru";
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(
                        this,
                        channelId
                )
                .setSmallIcon(
                        android.R.drawable.ic_dialog_email
                )
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(
                        NotificationCompat.PRIORITY_HIGH
                )
                .setVisibility(
                        NotificationCompat.VISIBILITY_PUBLIC
                )
                .setAutoCancel(true)
                .setContentIntent(
                        pendingIntent
                );

        manager.notify(
                (int) System.currentTimeMillis(),
                builder.build()
        );
    }

    @Override
    public void onNewToken(
            @NonNull String token
    ) {

        super.onNewToken(token);

        android.util.Log.i(
                TAG,
                "FCM TOKEN BARU = " + token
        );
    }
}
